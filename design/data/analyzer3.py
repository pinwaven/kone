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

    def plot_waveform(self, show_now=False, color='b', label=None, ax=None, mark_points=None):
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

            # 标记特定点
            # if mark_points:
            #     for point_x in mark_points:
            #         if point_x < len(self.data_biased):
            #             point_y = self.data_biased[int(point_x)]
            #             # 绘制标记点
            #             ax.plot(point_x, point_y, 'o', color=color, markersize=8, 
            #                    markeredgecolor='black', markeredgewidth=1)
                        
                        # 添加标注
                        # ax.annotate(f'({point_x}, {point_y:.1f})',
                        #            xy=(point_x, point_y),
                        #            xytext=(10, 10),
                        #            textcoords='offset points',
                        #            fontsize=9,
                        #            bbox=dict(boxstyle='round,pad=0.3', facecolor=color, alpha=0.3),
                        #            arrowprops=dict(arrowstyle='->', connectionstyle='arc3,rad=0'))
            
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
                           analyzer6, analyzer7, analyzer8, analyzer9, analyzer10):
    """将两个DataAnalyzer的数据绘制在同一张图中"""
    try:
        plt.rcParams["font.sans-serif"] = ["WenQuanYi Micro Hei"]
        plt.rcParams["axes.unicode_minus"] = False
        
        fig, ax = plt.subplots(figsize=(14, 8))
        
        # 绘制第一个分析器的数据（蓝色）
        ax1 = analyzer1.plot_waveform(color='#FF0000', label=f"{analyzer1.file_path.name}", ax=ax,mark_points=[370, 594])
        
        # 绘制第二个分析器的数据（红色）
        ax2 = analyzer2.plot_waveform(color='#008000', label=f"{analyzer2.file_path.name}", ax=ax, mark_points=[374, 595])
        
        # 绘制第三个分析器的数据（蓝色）
        ax3 = analyzer3.plot_waveform(color='#0000FF', label=f"{analyzer3.file_path.name}", ax=ax, mark_points=[370, 594])
        
        # 绘制第四个分析器的数据（红色）
        ax4 = analyzer4.plot_waveform(color='#FFA500', label=f"{analyzer4.file_path.name}", ax=ax, mark_points=[374, 595])
        
        # 绘制第一个分析器的数据（蓝色）
        ax5 = analyzer5.plot_waveform(color='#800080', label=f"{analyzer5.file_path.name}", ax=ax, mark_points=[370, 594])
        
        # 绘制第二个分析器的数据（红色）
        ax6 = analyzer6.plot_waveform(color='#00CCCC', label=f"{analyzer6.file_path.name}", ax=ax, mark_points=[374, 595])
        
        # 绘制第三个分析器的数据（蓝色）
        ax7 = analyzer7.plot_waveform(color='#FF00FF', label=f"{analyzer7.file_path.name}", ax=ax, mark_points=[370, 594])
        
        # 绘制第四个分析器的数据（红色）
        ax8 = analyzer8.plot_waveform(color='#BFBF00', label=f"{analyzer8.file_path.name}", ax=ax, mark_points=[374, 595])
        
        # 绘制第三个分析器的数据（蓝色）
        ax9 = analyzer9.plot_waveform(color='#FF80CC', label=f"{analyzer9.file_path.name}", ax=ax, mark_points=[370, 594])
        
        # 绘制第四个分析器的数据（红色）
        ax10 = analyzer10.plot_waveform(color='#006600', label=f"{analyzer10.file_path.name}", ax=ax, mark_points=[374, 595])
        
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




def plot_combined_waveform2(analyzer1, analyzer9):
    """将两个DataAnalyzer的数据绘制在同一张图中"""
    try:
        plt.rcParams["font.sans-serif"] = ["WenQuanYi Micro Hei"]
        plt.rcParams["axes.unicode_minus"] = False
        
        fig, ax = plt.subplots(figsize=(14, 8))
        
        # 绘制第一个分析器的数据（蓝色）
        ax1 = analyzer1.plot_waveform(color='#FF0000', label=f"{analyzer1.file_path.name}", ax=ax,mark_points=[370, 594])
        
        # 绘制第三个分析器的数据（蓝色）
        ax9 = analyzer9.plot_waveform(color='#FF80CC', label=f"{analyzer9.file_path.name}", ax=ax, mark_points=[370, 594])
        
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
        # analyzer1 = DataAnalyzer("4in1/1.bin")
        # analyzer2 = DataAnalyzer("4in1/2.bin")
        # analyzer3 = DataAnalyzer("4in1/3.bin")
        # analyzer4 = DataAnalyzer("4in1/4.bin")
        # analyzer5 = DataAnalyzer("4in1/5.bin")
        # analyzer6 = DataAnalyzer("4in1/6.bin")
        # analyzer7 = DataAnalyzer("4in1/7.bin")
        # analyzer8 = DataAnalyzer("4in1/8.bin")
        # analyzer9 = DataAnalyzer("4in1/9.bin")
        # analyzer10 = DataAnalyzer("4in1/10.bin")

        analyzer1 = DataAnalyzer("2in1/1.bin")
        analyzer2 = DataAnalyzer("2in1/2.bin")
        analyzer3 = DataAnalyzer("2in1/3.bin")
        analyzer4 = DataAnalyzer("2in1/4.bin")
        analyzer5 = DataAnalyzer("2in1/5.bin")
        analyzer6 = DataAnalyzer("2in1/6.bin")
        analyzer7 = DataAnalyzer("2in1/7.bin")
        analyzer8 = DataAnalyzer("2in1/8.bin")
        analyzer9 = DataAnalyzer("2in1/9.bin")
        analyzer10 = DataAnalyzer("2in1/10.bin")
        
    except FileNotFoundError as e:
        logger.error(f"文件未找到: {e}")
        return
    except ValueError as e:
        logger.error(f"文件处理错误: {e}")
        return
    except Exception as e:
        logger.error(f"初始化分析器时发生错误: {e}")

    startPoint1 = analyzer1.data_biased[0]
    startPoint2 = analyzer2.data_biased[0]
    startPoint3 = analyzer3.data_biased[0]
    startPoint4 = analyzer4.data_biased[0]
    startPoint5 = analyzer5.data_biased[0]
    startPoint6 = analyzer6.data_biased[0]
    startPoint7 = analyzer7.data_biased[0]
    startPoint8 = analyzer8.data_biased[0]
    startPoint9 = analyzer9.data_biased[0]
    startPoint10 = analyzer10.data_biased[0]

    startDiff2 = startPoint1 - startPoint2
    startDiff3 = startPoint1 - startPoint3
    startDiff4 = startPoint1 - startPoint4
    startDiff5 = startPoint1 - startPoint5
    startDiff6 = startPoint1 - startPoint6
    startDiff7 = startPoint1 - startPoint7
    startDiff8 = startPoint1 - startPoint8
    startDiff9 = startPoint1 - startPoint9
    startDiff10 = startPoint1 - startPoint10

    for i in range(len(analyzer2.data_biased)):
        analyzer2.data_biased[i] = analyzer2.data_biased[i] + startDiff2

    for i in range(len(analyzer3.data_biased)):
        analyzer3.data_biased[i] = analyzer3.data_biased[i] + startDiff3

    for i in range(len(analyzer4.data_biased)):
        analyzer4.data_biased[i] = analyzer4.data_biased[i] + startDiff4

    for i in range(len(analyzer5.data_biased)):
        analyzer5.data_biased[i] = analyzer5.data_biased[i] + startDiff5

    for i in range(len(analyzer6.data_biased)):
        analyzer6.data_biased[i] = analyzer6.data_biased[i] + startDiff6

    for i in range(len(analyzer7.data_biased)):
        analyzer7.data_biased[i] = analyzer7.data_biased[i] + startDiff7

    for i in range(len(analyzer8.data_biased)):
        analyzer8.data_biased[i] = analyzer8.data_biased[i] + startDiff8

    for i in range(len(analyzer9.data_biased)):
        analyzer9.data_biased[i] = analyzer9.data_biased[i] + startDiff9

    for i in range(len(analyzer10.data_biased)):
        analyzer10.data_biased[i] = analyzer10.data_biased[i] + startDiff10

    # 绘制组合波形
    plot_combined_waveform(analyzer1, analyzer2, analyzer3, analyzer4, analyzer5, 
                           analyzer6, analyzer7, analyzer8, analyzer9, analyzer10)

    # startPoint1 = analyzer1.data_biased[0]
    # startPoint9 = analyzer9.data_biased[0]
    # startDiff = startPoint1 - startPoint9

    # for i in range(len(analyzer9.data_biased)):
    #     analyzer9.data_biased[i] = analyzer9.data_biased[i] + startDiff

    # # 绘制组合波形
    # plot_combined_waveform2(analyzer1, analyzer9)


if __name__ == "__main__":
    main()