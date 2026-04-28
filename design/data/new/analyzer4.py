#!/usr/bin/env python3

import logging
import matplotlib.pyplot as plt
import numpy as np
import scipy
import struct
from pathlib import Path
from typing import Tuple, List, Optional

logging.basicConfig(
    level=logging.INFO, format="%(asctime)s - %(levelname)s @L%(lineno)d  %(message)s"
)
logger = logging.getLogger(__name__)


class DataAnalyzer:

    def __init__(self, file_path: str = ""):
        self.file_path = Path(file_path)
        self.data_len = 0
        self.laser_curr = 0
        self.data_bias = 0
        self.laser_curr_bias = 0

        self.raw_data = None
        self.data_biased = None

        if not self.file_path.exists():
            logger.error(f"File not found: {self.file_path}")
            raise FileNotFoundError(f"File not found: {self.file_path}")

        if not self.read_and_parse_file():
            logger.error("Failed to read and parse file")
            raise ValueError("Failed to read and parse file")

        if not self.preprocess_data():
            logger.error("Failed to process data")
            raise ValueError("Failed to process data")

    def read_and_parse_file(self) -> bool:
        """file format (little endian):
        byte 0-1: data_len (uint16_t)
        byte 2-3: laser_curr (12bit ADC in uint16_t)
        byte  4: fixed_5a (0x5a)
        byte 5-6: data_bias (uint16_t)
        byte 7-8: laser_curr_bias (uint16_t)
        byte  9: fixed_a5 (0xa5)
        byte 10-: raw data (13bit ADC in uint16_t)
        """
        try:
            if not self.file_path.exists():
                logger.error(f"file not found: {self.file_path}")
                return False

            with open(self.file_path, "rb") as f:
                header = f.read(10)
                if len(header) < 10:
                    logger.error("head < 10 bytes ?!")
                    return False

                (
                    self.data_len,
                    self.laser_curr,
                    fixed_5a,
                    self.data_bias,
                    self.laser_curr_bias,
                    fixed_a5,
                ) = struct.unpack("<HHBHHB", header)

                # check fixed bytes
                if fixed_5a != 0x5A:
                    logger.error(
                        f"fixed byte 0x5a check failed, actual: 0x{fixed_5a:02x}"
                    )
                    return False

                if fixed_a5 != 0xA5:
                    logger.error(
                        f"fixed byte 0xa5 check failed, actual: 0x{fixed_a5:02x}"
                    )
                    return False

                logger.info("head check passed")

                # read actual data
                remaining_data = f.read()
                if len(remaining_data) / 2 != self.data_len:
                    logger.warning(
                        f"data length mismatch: expected {self.data_len} bytes, actual {len(remaining_data)} bytes"
                    )
                    return False

                self.adc_values = self._retrieve_13bit_adc_data(remaining_data)
                if self.adc_values is False:
                    return False

                logger.info(f"data length: {self.data_len}")
                logger.info(f"laser current (raw): {self.laser_curr}")
                logger.info(f"data ADC bias: {self.data_bias}")
                logger.info(f"laser current bias: {self.laser_curr_bias}")
                return True

        except Exception as e:
            logger.error(f"error reading file: {e}")
            return False

    def _retrieve_13bit_adc_data(self, raw_data):
        if not raw_data:
            return False

        try:
            data_array = np.frombuffer(raw_data, dtype=np.uint16)

            mask_13bit = np.uint16(0x1FFF)
            invalid_mask = data_array > mask_13bit

            invalid_count = np.count_nonzero(invalid_mask)
            if invalid_count > 0:
                invalid_indices = np.where(invalid_mask)[0]
                invalid_values = data_array[invalid_indices]
                logger.error(f"found {invalid_count} invalid ADC values")
                return False

            logger.info(f"retrieved {len(data_array)} 13bit ADC data points")
            return data_array

        except Exception as e:
            logger.error(f"error processing ADC data: {e}")
            return False

    def preprocess_data(self) -> bool:

        # # 处理首部太小的数据
        # if self.adc_values is not None:
        #     valid_mask = self.adc_values >= self.data_bias
        #     if np.any(valid_mask):
        #         first_valid = np.argmax(valid_mask)
        #         if first_valid > 0:
        #             logger.info(f"trimming first {first_valid} values below bias")
        #             self.adc_values = self.adc_values[first_valid:]
        #     else:
        #         logger.warning("All values are below bias!")
        #         return False

        try:
            self.data_biased = self.adc_values.astype(np.int32) - np.int32(
                self.data_bias
            )

            self.laser_curr_biased = self.laser_curr - self.laser_curr_bias

            return True

        except Exception as e:
            logger.error(f"error processing data: {e}")
            return False

    def plot_waveform(self, show_now=False, color='b', label=None, ax=None, base_intervals=None):
        try:
            if ax is None:
                ax = plt.gca()
            
            time_axis = np.arange(len(self.data_biased))
            
            # 绘制波形
            ax.plot(time_axis, self.data_biased, color=color, linewidth=0.8, alpha=0.8, 
                   label=label if label else f"{self.file_path.name}")
            
            # 计算统计信息
            mean_val = np.mean(self.data_biased)
            std_val = np.std(self.data_biased)
            
            # 绘制统计线
            ax.axhline(y=mean_val, color=color, linestyle='--', alpha=0.7)
            ax.axhline(y=mean_val + std_val, color=color, linestyle=':', alpha=0.5)
            ax.axhline(y=mean_val - std_val, color=color, linestyle=':', alpha=0.5)

            # # 标记标准点base_intervals（使用固定颜色）
            # if base_intervals is not None:
            #     # 使用固定的颜色标识标准点
            #     base_color = 'red'  # 标准点使用红色
            #     for i, (start, end) in enumerate(base_intervals):
            #         # 标记起点和终点 - 使用实线
            #         plt.axvline(x=start, color=base_color, linestyle='-', alpha=0.8, 
            #                 linewidth=2, label='标准点起点' if i == 0 else "")
            #         plt.axvline(x=end, color=base_color, linestyle='-', alpha=0.8,
            #                 linewidth=2, label='标准点终点' if i == 0 else "")
                    
            #         # 在x轴上标记数值
            #         y_min, y_max = plt.ylim()
            #         label_y = y_min - (y_max - y_min) * 0.08  # 更靠下的位置
            #         plt.text(start, label_y, f'标准{start}', 
            #                 ha='center', va='top', fontsize=8, color=base_color,
            #                 bbox=dict(boxstyle='round,pad=0.2', facecolor='white', alpha=0.8))
            #         plt.text(end, label_y, f'标准{end}', 
            #                 ha='center', va='top', fontsize=8, color=base_color,
            #                 bbox=dict(boxstyle='round,pad=0.2', facecolor='white', alpha=0.8))
                    
            #         # 区间背景色 - 使用浅红色
            #         plt.axvspan(start, end, alpha=0.1, color=base_color)
                    
            #         # 在区间中点添加标签
            #         mid_point = (start + end) / 2
            #         plt.annotate(f'标准区间{i+1}', 
            #                     xy=(mid_point, y_max * 0.95),
            #                     xytext=(0, 0), textcoords='offset points',
            #                     ha='center', va='bottom', fontsize=9, color=base_color,
            #                     bbox=dict(boxstyle='round,pad=0.3', facecolor='white', alpha=0.9))

            # # 标记特定点
            # if mark_points:
            #     # 添加垂直参考线
            #     for point_x in mark_points:
            #         ax.axvline(x=point_x, color='gray', linestyle='--', alpha=0.5, linewidth=1)
            #         # # 在X轴上标记坐标
            #         # ax.text(point_x, ax.get_ylim()[0] + (ax.get_ylim()[1] - ax.get_ylim()[0]) * 0.02, 
            #         #     f'x={point_x}', fontsize=10, ha='center', va='bottom',
            #         #     bbox=dict(boxstyle='round,pad=0.2', facecolor='yellow', alpha=0.7))
            
            return ax

        except Exception as e:
            logger.error(f"error plotting waveform: {e}")
            return None

    def get_statistics(self) -> dict:

        data_array = np.array(self.data_biased)

        return {
            "count": len(self.data_biased),
            "mean": np.mean(data_array),
            "std": np.std(data_array),
            "min": np.min(data_array),
            "max": np.max(data_array),
            "median": np.median(data_array),
            "laser_current_corrected": self.laser_curr_biased,
            "data_adc_bias": self.data_bias,
            "laser_curr_bias": self.laser_curr_bias,
        }


def plot_combined_waveform(analyzer1, analyzer2, analyzer3, analyzer4, analyzer5, 
                           analyzer6, analyzer7, analyzer8, analyzer9):
    """将两个DataAnalyzer的数据绘制在同一张图中"""
    try:
        plt.rcParams["font.sans-serif"] = ["WenQuanYi Micro Hei"]
        plt.rcParams["axes.unicode_minus"] = False
        
        fig, ax = plt.subplots(figsize=(14, 8))
        
        # 绘制第一个分析器的数据（蓝色）
        ax1 = analyzer1.plot_waveform(color='#FF0000', label=f"{analyzer1.file_path.name}", ax=ax, base_intervals = [
        (173.0, 383.0),
        (450.0, 660.0),
        (686.0, 896.0),
        (966.0, 1176.0)
    ])
        
        # 绘制第二个分析器的数据（红色）
        ax2 = analyzer2.plot_waveform(color='#008000', label=f"{analyzer2.file_path.name}", ax=ax, base_intervals = [
        (173.0, 383.0),
        (450.0, 660.0),
        (686.0, 896.0),
        (966.0, 1176.0)
    ])
        
        # 绘制第三个分析器的数据（蓝色）
        ax3 = analyzer3.plot_waveform(color='#0000FF', label=f"{analyzer3.file_path.name}", ax=ax, base_intervals = [
        (173.0, 383.0),
        (450.0, 660.0),
        (686.0, 896.0),
        (966.0, 1176.0)
    ])
        
        # 绘制第四个分析器的数据（红色）
        ax4 = analyzer4.plot_waveform(color='#FFA500', label=f"{analyzer4.file_path.name}", ax=ax, base_intervals = [
        (173.0, 383.0),
        (450.0, 660.0),
        (686.0, 896.0),
        (966.0, 1176.0)
    ])
        
        # 绘制第一个分析器的数据（蓝色）
        ax5 = analyzer5.plot_waveform(color='#800080', label=f"{analyzer5.file_path.name}", ax=ax, base_intervals = [
        (173.0, 383.0),
        (450.0, 660.0),
        (686.0, 896.0),
        (966.0, 1176.0)
    ])
        
        # 绘制第二个分析器的数据（红色）
        ax6 = analyzer6.plot_waveform(color='#00CCCC', label=f"{analyzer6.file_path.name}", ax=ax, base_intervals = [
        (173.0, 383.0),
        (450.0, 660.0),
        (686.0, 896.0),
        (966.0, 1176.0)
    ])
        
        # 绘制第三个分析器的数据（蓝色）
        ax7 = analyzer7.plot_waveform(color='#FF00FF', label=f"{analyzer7.file_path.name}", ax=ax, base_intervals = [
        (173.0, 383.0),
        (450.0, 660.0),
        (686.0, 896.0),
        (966.0, 1176.0)
    ])
        
        # 绘制第四个分析器的数据（红色）
        ax8 = analyzer8.plot_waveform(color='#BFBF00', label=f"{analyzer8.file_path.name}", ax=ax, base_intervals = [
        (173.0, 383.0),
        (450.0, 660.0),
        (686.0, 896.0),
        (966.0, 1176.0)
    ])
        
        # 绘制第三个分析器的数据（蓝色）
        ax9 = analyzer9.plot_waveform(color='#FF80CC', label=f"{analyzer9.file_path.name}", ax=ax, base_intervals = [
        (173.0, 383.0),
        (450.0, 660.0),
        (686.0, 896.0),
        (966.0, 1176.0)
    ])
        
        # 设置图表标题和标签
        ax.set_title("两个数据文件的波形对比")
        ax.set_xlabel("时间（位置）")
        ax.set_ylabel("数值")
        ax.grid(True, alpha=0.3)
        ax.legend()
        
        plt.figtext(
            0.99,
            0.99,
            "",
            fontsize=9,
            verticalalignment='top',
            horizontalalignment='right',
            bbox=dict(
                boxstyle="round,pad=0.5",
                facecolor="wheat",
                alpha=0.25,
                edgecolor="gray",
                linewidth=0.5,
            ),
        )
        
        plt.tight_layout()
        plt.show()
        
    except Exception as e:
        logger.error(f"绘制组合波形时出错: {e}")


def main():
    try:
        analyzer1 = DataAnalyzer("15_4.bin")
        analyzer2 = DataAnalyzer("15_4_2.bin")
        analyzer3 = DataAnalyzer("20_4.bin")
        analyzer4 = DataAnalyzer("20_4_2.bin")
        analyzer5 = DataAnalyzer("25_4.bin")
        analyzer6 = DataAnalyzer("25_4_2.bin")
        analyzer7 = DataAnalyzer("30_4.bin")
        analyzer8 = DataAnalyzer("30_4_2.bin")
        analyzer9 = DataAnalyzer("empty.bin")
    except FileNotFoundError as e:
        logger.error(f"文件未找到: {e}")
        return
    except ValueError as e:
        logger.error(f"文件处理错误: {e}")
        return
    except Exception as e:
        logger.error(f"初始化分析器时发生错误: {e}")

    # 绘制组合波形
    plot_combined_waveform(analyzer1, analyzer2, analyzer3, analyzer4, analyzer5, 
                           analyzer6, analyzer7, analyzer8, analyzer9)


if __name__ == "__main__":
    main()