#!/usr/bin/env python3


import logging
import matplotlib.pyplot as plt
import numpy as np
import scipy
import scipy.signal
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

    def plot_waveform(self, show_now=False):
        try:
            # plt.style.use("./dark.style")
            plt.rcParams["font.sans-serif"] = ["WenQuanYi Micro Hei"]
            plt.rcParams["axes.unicode_minus"] = False

            time_axis = np.arange(len(self.data_biased))
            plt.figure(figsize=(12, 6))

            # main waveform plot - remove subplot since we're only using one plot
            plt.plot(time_axis, self.data_biased, "b-", linewidth=0.8, alpha=0.8)
            plt.title(f"采样波形 (偏置校正)")
            plt.xlabel("时间（位置）")
            plt.ylabel("数值")
            plt.grid(True, alpha=0.3)

            # statistics lines
            mean_val = np.mean(self.data_biased)
            std_val = np.std(self.data_biased)
            plt.axhline(
                y=mean_val,
                color="r",
                linestyle="--",
                alpha=0.7,
                label=f"均值: {mean_val:.2f}",
            )
            plt.axhline(
                y=mean_val + std_val,
                color="lightblue",  # 浅蓝
                linestyle=":",
                alpha=0.7,
                label=f"+1σ: {mean_val + std_val:.2f}",
            )
            plt.axhline(
                y=mean_val - std_val,
                color="orange",
                linestyle=":",
                alpha=0.7,
                label=f"-1σ: {mean_val - std_val:.2f}",
            )
            plt.legend()

            # 添加文本信息到右上角
            info_text = f"""数据信息:
• 数据点数: {len(self.data_biased)}
• 激光电流(校正): {self.laser_curr_biased}
• ADC偏置: {self.data_bias}
• 激光电流偏置: {self.laser_curr_bias}
• 数据范围: {min(self.data_biased)} ~ {max(self.data_biased)}
• 均值: {mean_val:.2f}
• 标准差: {std_val:.2f}"""

            plt.figtext(
                0.99,  # 右边位置 (0.98 表示靠近右边)
                0.99,  # 顶部位置 (0.98 表示靠近顶部)
                info_text,
                fontsize=9,
                verticalalignment="top",  # 从顶部开始对齐
                horizontalalignment="right",  # 右对齐
                bbox=dict(
                    boxstyle="round,pad=0.5",
                    facecolor="wheat",
                    alpha=0.25,  # 调整透明度 (0-1, 0完全透明, 1完全不透明)
                    edgecolor="gray",
                    linewidth=0.5,
                ),
            )

            plt.show(block=show_now)

        except Exception as e:
            logger.error(f"error plotting waveform: {e}")

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

    def signal_analysis_hba1c_cysc(self):
        data_array = np.array(self.data_biased)
        
        try:
            # 第一步：找到所有主峰和右边界
            peaks, peak_prop = scipy.signal.find_peaks(
                data_array, 
                distance=80, 
                prominence=30,
                width=10
            )
            
            # 检查峰值数量
            if len(peaks) != 4:
                logger.error(f"未能发现四个波峰，实际发现 {len(peaks)} 个波峰")
                return False
                
            logger.info(f"发现四个波峰，位置: {peaks}")
            
            # 获取峰值的基底信息
            if 'left_bases' not in peak_prop or 'right_bases' not in peak_prop:
                logger.error("峰值分析未能获取基底信息")
                return False
            
            # 第二步：确定所有右边界（使用find_peaks的结果）
            right_bases = peak_prop['right_bases']
            logger.info(f"右边界位置: {right_bases}")
            
            # 第三步：重新计算左边界，使用斜率变化检测
            waves = []
            for i in range(len(peaks)):
                peak_idx = peaks[i]
                right_base = right_bases[i]
                
                # 确定搜索左边界的范围
                if i == 0:
                    search_start = 0
                else:
                    search_start = right_bases[i-1]
                
                # 寻找真正的左边界
                left_base = self._find_slope_change_left_base(
                    data_array, peak_idx, search_start
                )
                
                waves.append({
                    'peak_index': peak_idx,
                    'left_base': left_base,
                    'right_base': right_base,
                    'peak_value': data_array[peak_idx]
                })
                
                logger.info(f"波包 {i+1}: 峰值位置={peak_idx}, 左基底={left_base}, 右基底={right_base}")
            
            # 绘制分析结果
            self._plot_wave_analysis(data_array, waves)
            
            return {
                'wave_count': len(waves),
                'waves': waves,
                'analysis_success': True
            }
            
        except Exception as e:
            logger.error(f"信号分析过程中发生错误: {e}")
            return False
    
    def _find_slope_change_left_base(self, data, peak_idx, search_start):
        """
        使用斜率变化检测波包的真正起点
        """
        try:
            # 确保搜索范围合理
            search_start = max(0, search_start)
            search_end = peak_idx
            
            if search_end - search_start < 20:
                return search_start
            
            # 提取搜索段数据
            segment = data[search_start:search_end]
            
            # 计算斜率（一阶导数）
            slopes = np.diff(segment)
            
            if len(slopes) == 0:
                return search_start
            
            # 设置斜率阈值为3.0（更高的阈值）
            slope_threshold = 3.0
            
            # 从左向右找第一个斜率大于阈值的点
            for i, slope in enumerate(slopes):
                if slope > slope_threshold:
                    # 找到了斜率剧烈变化的位置
                    # 再向前回退几个点，确保捕获波包的真正起始
                    left_base = search_start + max(0, i - 5)
                    if left_base >= search_start and left_base < peak_idx:
                        return left_base
            
            # 如果没找到，返回搜索起点
            return search_start
            
        except Exception as e:
            logger.warning(f"斜率变化检测时出错: {e}")
            return search_start

    def _plot_wave_analysis(self, data_array, waves):
        """绘制波包分析结果"""
        try:
            plt.rcParams["font.sans-serif"] = ["WenQuanYi Micro Hei"]
            plt.rcParams["axes.unicode_minus"] = False
            plt.figure(figsize=(14, 8))
            
            # 绘制原始数据
            time_axis = np.arange(len(data_array))
            plt.plot(time_axis, data_array, 'b-', linewidth=0.8, alpha=0.7, label='原始数据')
            
            # 绘制每个波包
            colors = ['red', 'green', 'orange', 'purple']
            for i, wave in enumerate(waves):
                color = colors[i % len(colors)]
                
                # 标记峰值
                plt.plot(wave['peak_index'], wave['peak_value'], 'o', 
                        color=color, markersize=8, label=f'峰值 {i+1}')
                
                # 标记基底范围
                plt.axvspan(wave['left_base'], wave['right_base'], 
                           alpha=0.2, color=color, label=f'波包 {i+1}')
                
                # 添加峰值标注
                plt.annotate(f'P{i+1}\n({wave["peak_index"]}, {wave["peak_value"]:.1f})',
                           xy=(wave['peak_index'], wave['peak_value']),
                           xytext=(10, 10), textcoords='offset points',
                           fontsize=9, ha='left',
                           bbox=dict(boxstyle='round,pad=0.3', facecolor=color, alpha=0.3))
            
            plt.title('HbA1c和胱抑素C信号分析 - 波包检测')
            plt.xlabel('时间（位置）')
            plt.ylabel('数值')
            plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left')
            plt.grid(True, alpha=0.3)
            plt.tight_layout()
            plt.show()
            
        except Exception as e:
            logger.error(f"绘图过程中发生错误: {e}")

    def signal_analysis_hba1c_cysc_old(self):
        # 旧的糖化血红蛋白和胱抑素C分析方法
        data_array = np.array(self.data_biased)

        result = dict()

        # 找到峰值
        peaks, _ = scipy.signal.find_peaks(data_array, distance=80, prominence=30)

        result["peak_analysis"] = {
            "peak_count": len(peaks),
            "peak_indices": peaks,
            "peak_values": data_array[peaks] if len(peaks) > 0 else [],
        }

        logger.info("糖化血红蛋白和胱抑素C分析：")
        if "peak_analysis" in result:
            peak_info = result["peak_analysis"]
            logger.info(f"  峰值分析: 发现 {peak_info['peak_count']} 个峰值")

            # 绘制峰值
            plt.figure(figsize=(12, 6))
            plt.plot(data_array, label="原始数据", alpha=0.5)
            plt.plot(
                peak_info["peak_indices"], peak_info["peak_values"], "ro", label="峰值"
            )
            plt.title("峰值分析")
            plt.xlabel("时间（位置）")
            plt.ylabel("数值")
            plt.legend()
            plt.show()


def main():
    try:
        analyzer = DataAnalyzer("break10_3.bin")
    except FileNotFoundError as e:
        logger.error(f"文件未找到: {e}")
        return
    except ValueError as e:
        logger.error(f"文件处理错误: {e}")
        return
    except Exception as e:
        logger.error(f"初始化分析器时发生错误: {e}")
        return

    stats = analyzer.get_statistics()
    logger.info("数据统计信息:")
    for key, value in stats.items():
        logger.info(f"  {key}: {value}")

    analyzer.plot_waveform()
    analyzer.signal_analysis_hba1c_cysc()
    plt.show()


if __name__ == "__main__":
    main()
    # stats = analyzer.get_statistics()
    # logger.info("数据统计信息:")
    # for key, value in stats.items():
    #     logger.info(f"  {key}: {value}")

    # analyzer.plot_waveform()

    # # rev = analyzer.signal_analysis_hba1c_cysc_old()
    # analyzer.signal_analysis_hba1c_cysc()
    
    plt.show()

