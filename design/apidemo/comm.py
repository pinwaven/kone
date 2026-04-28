#!/usr/bin/env python3

import anycrc
import enum
import fcntl
import os
import re
import select
import serial
import time

try:
    from rich.console import Console

    rich_console = Console()

    def plain_print(*args, **kwargs):
        kwargs.setdefault("highlight", False)
        kwargs.setdefault("emoji", True)
        return rich_console.print(*args, **kwargs)

    print = plain_print

except ImportError:
    print("rich module not found, using plain print instead.")
    rich = None


class NODE(enum.Enum):
    A = 0
    B = 1


class CMD(enum.Enum):
    POLL = b"\x01"
    HI = b"\x02"
    TEST = b"\x03"
    REG_WRITE = b"\x04"
    REG_READ = b"\x05"
    GPIO_READ = b"\x06"
    GPIO_WRITE = b"\x07"
    A_CANCEL = b'\x08'
    
    A_HOMING = b'\x40'
    A_MOVE_DURATION = b'\x41'
    A_MOVE_TO_SS = b'\x42'
    A_ABSORB = b'\x43'
    A_SCAN = b'\x44'
    A_QUERY_DATA = b'\x45'
    A_READ_QR = b'\x46'

class GPIO(enum.Enum):
    SS_PWR = 0
    SS_UD = 1
    SS_START = 2
    SS_STOP = 3
    SS_CARD = 4
    LD_PWR = 5
    QR_TGL = 6
    QR_RST = 7

class TMC_REG(enum.Enum):
    # General REGs
    GCONF = 0x00
    GSTAT = 0x01
    IFCNT = 0x02
    NODECONF = 0x03
    IOIN = 0x06

    # Velocity Dependent REGs
    IHOLD_IRUN = 0x10
    TPOWERDOWN = 0x11
    TSTEP = 0x12
    VACTUAL = 0x22
    
    # StallGuard REGs
    TCOOLTHRS = 0x14
    SGTHRS = 0x40
    SG_VALUE = 0x41
    
    # COOLCONF REGs
    COOLCONF = 0x42
    
    # Sequencer REGs
    MSCNT = 0x6A
    
    # Chopper Control REGs
    CHOPCONF = 0x6C
    DRV_STATUS = 0x6F
    PWMCONF = 0x70
    PWM_SCALE = 0x71
    PWM_AUTO = 0x72


TMC_REG_BITMAPS = {
    TMC_REG.GCONF: {
        0: "reserved_0",
        1: "extcap",
        2: "reserved_1",
        3: "shaft",
        4: "diag_index",
        5: "diag_step",
        6: "multistep_filt",
        7: "test_mode",
    },
    TMC_REG.GSTAT: {
        0: "reset",
        1: "drv_err",
        2: "u3v5",
    },
    TMC_REG.IOIN: {
        0: "en",
        1: "nSTDBY",
        2: "AD0",
        3: "AD1",
        4: "DIAG",
        5: "STEPPER",
        6: "PDN_UART",
        7: "MODE",
        8: "STEP",
        9: "DIR",
        10: "COMP_A1A2",
        11: "COMP_B1B2",
        (24, 31): "VERSION",
    },
    TMC_REG.IHOLD_IRUN: {
        (0, 4): "IHOLD",
        (8, 12): "IRUN",
        (16, 19): "IHOLDDELAY"
    },
    TMC_REG.COOLCONF: {
        (0, 3): "mini SG value for smart current control",
        4: "reserved_0",
        (5, 6): "current up step width",
        7: "reserved_1",
        (8, 11): "SG hysteresis value for smart current control",
        12: "reserved_2",
        (13, 14): "current down step speed",
        15: "minimum current for smart current control",
    },
    TMC_REG.CHOPCONF: {
        0: "enabledrv",
        (1, 14): "reserved_0",
        (15, 16): "blank time select",
        (17, 23): "reserved_1",
        (24, 27): "micro step resolution",
        28: "interpolation to 256 microsteps",
        29: "double edge",
        30: "short to GND protection disable",
        31: "low side short protection disable"
    },
    TMC_REG.PWMCONF: {
        (0, 7): "PWM_OFS",
        (8, 15): "PWM_GRAD",
        (16, 17): "pwm_freq",
        18: "pwm_autoscale",
        19: "pwm_autograd",
        (20, 21): "freewheel",
        (22, 23): "reserved_0",
        (24, 27): "PWM_REG",
        (28, 31): "PWM_LIM"
    },
    TMC_REG.DRV_STATUS: {
        0: "over temperature pre-warning flag",
        1: "over temperature flag",
        2: "short to ground on A",
        3: "short to ground on B",
        4: "low side short on A",
        5: "low side short on B",
        6: "open load on A",
        7: "open load on B",
        8: "120 degree",
        9: "150 degree",
        (10, 15): "reserved_0",
        (16, 20): "actual motor current / smart current current",
        (21, 30): "reserved_1",
        31: "standstill indicator"
    },
    TMC_REG.PWM_SCALE: {
        (0, 7): "pwm scale sum",
        (16, 24): "pwm scale auto"
    },
    TMC_REG.PWM_AUTO: {
        (0, 7): "pwm ofs auto",
        (16, 23): "pwm gradient auto",
    },
}



class InterByteTimeoutSerial(serial.Serial):
    def read(self, size=1):
        read_data = bytearray()
        start_time = time.time()
        last_recv_time = None

        # print('\t', end='')
        while len(read_data) < size:
            now = time.time()

            if self.timeout and (now - start_time) > self.timeout:
                break

            if self.inter_byte_timeout and last_recv_time is not None:
                if (now - last_recv_time) > self.inter_byte_timeout:
                    break

            rlist, _, _ = select.select(
                [self.fileno()], [], [], self.inter_byte_timeout
            )

            if rlist:
                b = os.read(self.fileno(), 1)
                if b:
                    read_data += b
                    last_recv_time = time.time()
                    print("[grey27].[/grey27]", end="")
                else:
                    break
            else:
                continue

        return bytes(read_data)


""" a decorator to mark external interface functions
    it give a func a _external_if_ attribute
"""
def exter_if(func):
    func._external_if_ = True
    return func


class Comm:
    
    def __init__(self, dev_name='AUTO'):
        # dev_name = "/dev/serial/by-id/usb-1a86_USB2.0-Serial-if00-port0"
        self.crcfunc = anycrc.CRC(
            width=8, poly=0x07, init=0x00, xorout=0x00, refin=True, refout=False
        )
        self.addr = b"!"
        self.dev_name = dev_name
        
        self.dev = None
        self.connect()

    def connect(self):
    
        if self.dev:
            self.dev_name = 'AUTO'
            try:
                self.dev.close()
            except Exception as e:
                print(f"Error closing existing serial connection: {e}")
            
            time.sleep(1)
    
        if self.dev_name == 'AUTO':
            from serial.tools import list_ports
            self.dev_name = None
            for port in list_ports.comports():
                if port.name.find('ACM') >= 0:
                    self.dev_name = port.device
                    break
                if port.name.find('USB') >= 0:
                    self.dev_name = port.device
                    break
            if not self.dev_name:
                raise ValueError("No suitable serial port found. Please specify a valid port name.")
            print(f'[yellow]Using serial port: {self.dev_name}[/yellow]')
        elif not os.path.exists(self.dev_name):
            raise ValueError(f"Serial port {self.dev_name} does not exist. Please specify a valid port name.")
            
        BAUD = 460800 / 2

        try:
            self.dev = InterByteTimeoutSerial(
                port=self.dev_name, baudrate=BAUD, timeout=1, inter_byte_timeout=0.01
            )
            print(f'[green]Successfully connected to {self.dev_name}[/green]')
        except Exception as e:
            print(f'[red]Failed to connect to {self.dev_name}: {e}[/red]')
            self.dev = None
            raise

    def send_recv(self, data):
        if isinstance(data, str):
            data = data.encode("latin-1")

        frame = self.addr + data
        crc = self.crcfunc.calc(frame)
        frame += bytes([crc])
        # frame_len = len(frame)
        
        _write_done = False
        for i in range(3):
            try:
                print(f"[grey35]{frame.hex()}[/grey35]", end="")
                self.dev.write(frame)
                _write_done = True
                break
            except Exception as e:
                print(f"error on writing to serial port: {e}")
                try:
                    self.connect()
                except Exception as e:
                    print(f"reconnect failed: {e}")
                time.sleep(0.5)
        if not _write_done:
            return False

        data = self.dev.read(10240)
        if len(data) > 0:
            # print('\n' + ' '*frame_len*2, end='')
            s = data.decode("latin-1", errors="replace")
            s = re.sub(r"[^\x20-\x7e]", "囗", s)
            print(f"[grey35]{s}[/grey35]")
        else:
            print('[grey35]\nno data received[/grey35]')
            return False

        # check CRC
        if len(data) < 3 or data[-1] != self.crcfunc.calc(data[:-1]):
            print("CRC check failed, crc:", data[-1], "expected:", self.crcfunc.calc(data[:-1]))
            return False

        return data[2:-1]  # remove address byte, '|' , and CRC byte

    def parse_register(self, reg, value):
        if reg not in TMC_REG_BITMAPS:
            return value
        bitmap = TMC_REG_BITMAPS[reg]
        parsed = {}
        for bit, name in bitmap.items():
            if isinstance(bit, tuple):
                start, end = bit
                mask = (1 << (end - start + 1)) - 1
                parsed[name] = (value >> start) & mask
            else:
                parsed[name] = (value >> bit) & 0x1
        return parsed

    def tmc_reg_read(self, node, reg):

        s = CMD.REG_READ.value
        s += node.value.to_bytes(1)
        s += reg.value.to_bytes(1)

        data = self.send_recv(s)
        if not data:
            return False

        data = data.decode("latin-1").split(",")
        if len(data) != 3:
            print("Invalid data format:", data)
            return False
        node_str, reg_str, val_str = data
        
        if val_str.startswith("0x"):
            val = int(val_str, 16)
            return val
        else:
            return False
        
    def tmc_reg_write(self, node, reg, value):
        
        s = CMD.REG_WRITE.value
        s += node.value.to_bytes(1)
        s += reg.value.to_bytes(1)
        if value < 0:
            s += value.to_bytes(4, 'big', signed=True)
        else:
            s += value.to_bytes(4, 'big', signed=False)

        data = self.send_recv(s)
        if not data:
            return False

        data = data.decode("latin-1").split(",")
        if len(data) != 3:
            print("invalid data format:", data)
            return False
        node_str, reg_str, val_str = data
        
        if val_str.startswith("0x"):
            val = int(val_str, 16)
            return val
        else:
            return False

    @exter_if
    def poll(self):
        s = CMD.POLL.value
        data = self.send_recv(s)
        if not data:
            return False

        data = data.decode("latin-1").split(",")
        return data

    @exter_if
    def hi(self):
        s = CMD.HI.value
        data = self.send_recv(s)
        if not data:
            return False

        data = data.decode("latin-1").split(":")[-1]
        return data

    @exter_if
    def test(self):
        s = CMD.TEST.value
        data = self.send_recv(s)
        if not data:
            return False
        
        return data.decode("latin-1").strip()

    @exter_if
    def gpio_read(self):
        s = CMD.GPIO_READ.value
        data = self.send_recv(s)
        if not data:
            return False

        data = data.decode("latin-1").split(",")
        return data
    
    @exter_if
    def gpio_write(self, pin, value):
        if isinstance(pin, str):
            pin = pin.upper()
            pin = GPIO[pin]
        elif isinstance(pin, int):
            pin = GPIO(pin)

        if isinstance(value, str):
            if value.startswith("0x"):
                value = int(value, 16)
            else:
                value = int(value)
        elif isinstance(value, int):
            value = value
        
        s = CMD.GPIO_WRITE.value
        s += pin.value.to_bytes(1)
        s += value.to_bytes(1)

        data = self.send_recv(s)
        if not data:
            return False

        data = data.decode("latin-1").split(":")
        if len(data) != 2:
            print("invalid data format:", data)
            return False
        
        return data
        

    @exter_if
    def read(self, node, reg):
        if isinstance(node, str):
            node = node.upper()
            node = NODE[node]
        elif isinstance(node, int):
            node = NODE(node)
        
        if isinstance(reg, str):
            reg = reg.upper()
            reg = TMC_REG[reg]
        elif isinstance(reg, int):
            reg = TMC_REG(reg)
        # return self.tmc_reg_read(node, reg)
        
        val = self.tmc_reg_read(node, reg)
        if val is False:
            return f'failed to read register {reg.name} on node {node.name}'
        
        reg_enum = TMC_REG(reg)
        parsed_value = self.parse_register(reg_enum, val)
        formatted_output = f">> {node.name} reg:{reg_enum.name} -> 0x{val:02X} <-\n"
        if isinstance(parsed_value, dict):
            formatted_output += '\n'
            max_key_length = max(len(k) for k in parsed_value.keys())
            for key, value in parsed_value.items():
                formatted_output += f"\t{key:<{max_key_length}}: {hex(value)}\n"
        else:
            formatted_output += f"0x{parsed_value:02X} <-\n"
        return formatted_output.strip()
    
    @exter_if
    def write(self, node, reg, value):
        if isinstance(node, str):
            node = node.upper()
            node = NODE[node]
        elif isinstance(node, int):
            node = NODE(node)
        
        if isinstance(reg, str):
            reg = reg.upper()
            reg = TMC_REG[reg]
        elif isinstance(reg, int):
            reg = TMC_REG(reg)

        if isinstance(value, str):
            if value.startswith("0x"):
                value = int(value, 16)
            else:
                value = int(value)
        elif isinstance(value, int):
            value = value
        
        val = self.tmc_reg_write(node, reg, value)
        if val is False:
            return f'failed to write register {reg.name} on node {node.name}'
        
        return f"wrote {hex(value)} to {node.name}, reg:{reg.name} -> {hex(val)}"
    
    @exter_if
    def cancel(self):
        s = CMD.A_CANCEL.value
        data = self.send_recv(s)
        if not data:
            return False

        return data
    
    @exter_if
    def homing(self):
        s = CMD.A_HOMING.value
        data = self.send_recv(s)
        if not data:
            return False

        return data
    
    @exter_if
    def move_dur(self, node, velocity, duration):
        if isinstance(node, str):
            node = node.upper()
            node = NODE[node]
        elif isinstance(node, int):
            node = NODE(node)
            
        velocity = int(velocity)
        duration = int(duration)

        s = CMD.A_MOVE_DURATION.value
        s += node.value.to_bytes(1)
        s += velocity.to_bytes(4, 'little', signed=True)
        s += duration.to_bytes(4, 'little', signed=False)

        data = self.send_recv(s)
        if not data:
            return False

        return data.decode("latin-1").strip()

    @exter_if
    def move_to_ss(self, node, velocity, duration, ss_id):
        if isinstance(node, str):
            node = node.upper()
            node = NODE[node]
        elif isinstance(node, int):
            node = NODE(node)
            
        velocity = int(velocity)
        duration = int(duration)
        ss_id = int(ss_id)

        s = CMD.A_MOVE_TO_SS.value
        s += node.value.to_bytes(1)
        s += velocity.to_bytes(4, 'little', signed=True)
        s += duration.to_bytes(4, 'little', signed=False)
        s += ss_id.to_bytes(1, 'little', signed=False)

        data = self.send_recv(s)
        if not data:
            return False

        return data.decode("latin-1").strip()

    @exter_if
    def scan(self, velocity, duration):
        velocity = int(velocity)
        duration = int(duration)

        s = CMD.A_SCAN.value
        s += velocity.to_bytes(4, 'little', signed=True)
        s += duration.to_bytes(4, 'little', signed=False)

        data = self.send_recv(s)
        if not data:
            return False

        return data.decode("latin-1").strip()

    @exter_if
    def query(self):
        s = CMD.A_QUERY_DATA.value
        data = self.send_recv(s)
        if not data:
            return False

        # dump data to a file
        f_name = "data4in1.bin"
        with open(f_name, "wb") as f:
            f.write(data)
        print(f"[green]Data saved to {f_name}[/green]")
        return f_name
    
    @exter_if
    def qr(self):
        s = CMD.A_READ_QR.value
        data = self.send_recv(s)
        if not data:
            return False

        # decode the QR code data
        try:
            data = data.decode("latin-1").strip()
            return data
        except Exception as e:
            print(f"[red]Failed to decode QR data: {e}[/red]")
            return False

if __name__ == "__main__":
    comm = Comm()
    # print(comm.read('A', 'IOIN'))
    # print(comm.send_recv('\x02'))
    print(comm.poll())
    print(comm.hi())
