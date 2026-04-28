#!/usr/bin/env python3
"""
ma.py

Monitor /run/shm/data.bin and overlay new waveforms on a persistent matplotlib figure
when the file changes. Press 'R' (or 'r') in the plot window to clear all plots.

Usage: python3 ma.py
"""

import ctypes
import logging
import matplotlib.pyplot as plt
import os
import queue
import sys
import threading
import time
from itertools import cycle
from matplotlib import colors as mcolors
from pathlib import Path

from analyzer import DataAnalyzer

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

DATA_PATH = Path("2in1/1_1.bin")
POLL_INTERVAL_MS = 500  # check file every 500ms


def make_color_cycle():
    # use high-contrast colors on white background, filtering out light colors
    import matplotlib.colors as mcolors
    
    # Start with tableau colors (generally good contrast)
    good_colors = list(mcolors.TABLEAU_COLORS.values())
    
    # Add selected CSS4 colors with good contrast on white
    dark_css4 = ['darkred', 'darkblue', 'darkgreen', 'darkorange', 'darkviolet', 
                 'brown', 'navy', 'maroon', 'forestgreen', 'indigo', 'crimson',
                 'steelblue', 'darkslategray', 'purple', 'darkgoldenrod']
    
    for color_name in dark_css4:
        if color_name in mcolors.CSS4_COLORS:
            good_colors.append(mcolors.CSS4_COLORS[color_name])
    
    return cycle(good_colors)


def get_line_width_for_color(color):
    """Calculate line width based on color brightness (lighter colors get thicker lines)"""
    import matplotlib.colors as mcolors
    
    # Convert color to RGB if it's a hex string
    if isinstance(color, str) and color.startswith('#'):
        rgb = mcolors.hex2color(color)
    elif isinstance(color, str):
        rgb = mcolors.to_rgb(color)
    else:
        rgb = color
    
    # Calculate perceived brightness (0-1, where 1 is white)
    brightness = 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2]
    
    # Map brightness to line width (brighter colors get thicker lines)
    # Range from 0.8 (dark colors) to 2.0 (light colors)
    line_width = 0.8 + (brightness * 1.2)
    return line_width


class LivePlot:
    def __init__(self, data_path: Path):
        self.data_path = data_path
        self.last_mtime = None
        self.lines = []  # matplotlib Line2D objects
        self.color_cycle = make_color_cycle()

        self.fig, self.ax = plt.subplots(figsize=(12, 6))
        self.ax.set_title("Live waveform monitor")
        self.ax.set_xlabel("Time (position)")
        self.ax.set_ylabel("Value")
        self.ax.grid(True, alpha=0.3)

        # connect key press event
        self.fig.canvas.mpl_connect("key_press_event", self._on_key)

        # set up an inotify watcher (netlink) in a background thread and a small UI timer
        self.event_queue = queue.Queue()
        try:
            self.watcher = InotifyWatcher(str(self.data_path), self.event_queue)
            self.watcher.start()
        except Exception as e:
            logger.warning(f"Inotify watcher failed to start ({e}), falling back to mtime polling")
            # fallback to simple polling using the existing timer
            self.timer = self.fig.canvas.new_timer(interval=POLL_INTERVAL_MS)
            self.timer.add_callback(self._poll_file)
            self.timer.start()

        # UI timer checks the event queue and triggers updates (lightweight)
        self.ui_timer = self.fig.canvas.new_timer(interval=200)
        self.ui_timer.add_callback(self._check_events)
        self.ui_timer.start()

    def _on_key(self, event):
        # clear plots on R/r
        if not event.key:
            return
        if event.key.lower() == "r":
            logger.info("Clear command received: clearing all plotted lines")
            self._clear()
            self.fig.canvas.draw_idle()

    def _clear(self):
        # remove lines and reset axes
        for ln in self.lines:
            try:
                ln.remove()
            except Exception:
                pass
        self.lines = []
        self.ax.cla()
        self.ax.set_title("Live waveform monitor")
        self.ax.set_xlabel("Time (position)")
        self.ax.set_ylabel("Value")
        self.ax.grid(True, alpha=0.3)

    def _poll_file(self):
        try:
            if not self.data_path.exists():
                # nothing to do
                return
            mtime = self.data_path.stat().st_mtime
            if self.last_mtime is None:
                # initial load
                self.last_mtime = mtime
                self._update_plot()
            elif mtime != self.last_mtime:
                logger.info("Detected change in data file, updating plot")
                self.last_mtime = mtime
                self._update_plot()
        except Exception as e:
            logger.exception(f"Error polling file: {e}")

    def _check_events(self):
        # called in the GUI thread periodically; drain event queue and update once per drain
        try:
            had = False
            while True:
                try:
                    _ = self.event_queue.get_nowait()
                    had = True
                except queue.Empty:
                    break
            if had:
                logger.info("Detected change via inotify, updating plot")
                self._update_plot()
        except Exception as e:
            logger.exception(f"Error checking inotify queue: {e}")

    def _update_plot(self):
        try:
            analyzer = DataAnalyzer(str(self.data_path))
        except FileNotFoundError:
            logger.warning("Data file not found while updating plot")
            return
        except Exception as e:
            logger.exception(f"Failed to parse data for plotting: {e}")
            return

        if analyzer.data_biased is None:
            logger.warning("No preprocessed data available to plot")
            return

        data = analyzer.data_biased
        time_axis = range(len(data))

        color = next(self.color_cycle)
        line_width = get_line_width_for_color(color)
        label = f"t={time.strftime('%H:%M:%S')}"
        ln, = self.ax.plot(time_axis, data, color=color, linewidth=line_width, alpha=0.9, label=label)
        self.lines.append(ln)

        # draw legend with many entries handled
        self.ax.legend(loc="upper right", fontsize='small')
        self.fig.canvas.draw_idle()


class InotifyWatcher(threading.Thread):
    """A tiny inotify watcher implemented with ctypes/syscalls to avoid extra deps.
    It watches a single path for MODIFY events and pushes a marker into the provided queue.
    """

    # inotify constants
    IN_MODIFY = 0x00000002
    IN_ATTRIB = 0x00000004
    IN_CLOSE_WRITE = 0x00000008

    def __init__(self, path: str, q: "queue.Queue"):
        super().__init__(daemon=True)
        self.path = path
        self.q = q
        self._stop = threading.Event()

        # libc syscall wrappers
        self.libc = ctypes.CDLL("libc.so.6")
        self.inotify_init = self.libc.inotify_init1
        self.inotify_init.argtypes = [ctypes.c_int]
        self.inotify_init.restype = ctypes.c_int
        self.inotify_add_watch = self.libc.inotify_add_watch
        self.inotify_add_watch.argtypes = [ctypes.c_int, ctypes.c_char_p, ctypes.c_uint32]
        self.inotify_add_watch.restype = ctypes.c_int

    def run(self):
        # init inotify with non-blocking flag if available
        IN_CLOEXEC = 0x00080000
        try:
            fd = self.inotify_init(IN_CLOEXEC)
        except Exception:
            fd = self.inotify_init(0)
        if fd < 0:
            logger.warning("inotify init failed")
            return

        wd = self.inotify_add_watch(fd, self.path.encode("utf-8"), self.IN_MODIFY | self.IN_CLOSE_WRITE | self.IN_ATTRIB)
        if wd < 0:
            logger.warning(f"inotify add_watch failed for {self.path}")
            return

        # read loop
        buf_size = 1024
        read = os.read
        while not self._stop.is_set():
            try:
                # blocking read with small timeout behavior via non-blocking fd would be more complex;
                # simple approach: poll using select with timeout
                import select

                rlist, _, _ = select.select([fd], [], [], 0.5)
                if fd in rlist:
                    data = read(fd, buf_size)
                    if data:
                        # push an event marker
                        try:
                            self.q.put_nowait(True)
                        except queue.Full:
                            pass
            except Exception:
                # keep loop running until stopped
                continue

    def stop(self):
        self._stop.set()


def main():
    lp = LivePlot(DATA_PATH)
    logger.info("Starting live plot. Press 'R' in the figure window to clear plots.")
    plt.show()


if __name__ == '__main__':
    main()
