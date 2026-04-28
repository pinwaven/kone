package poct.device.app.utils.app

import timber.log.Timber

object AppBatteryUtils {
    // 配置参数
    private const val LOCKED_PERCENT = 20f      // 锁定的电量百分比
    private const val MAX_PERCENT = 100f        // 最大百分比
    private const val HIGH_PERCENT_START = 90f  // 高电量开始点

    // 状态变量
    @Volatile
    private var filteredDisplayPercent = 0f

    @Volatile
    private var lastActualPercent = 0f

    @Volatile
    private var lastDisplayPercent = 0f

    @Volatile
    private var currentActualPercent = 0f

    @Volatile
    private var isFirstUpdate = true           // 首次更新标志

    // 平滑滤波参数
    private const val ALPHA = 0.3f              // 低通滤波系数
    private const val HYSTERESIS_THRESHOLD = 2f // 滞后阈值

    // 调试标志
    private const val DEBUG = true

    /**
     * 更新实际电量并计算显示电量（主方法）
     * 为了解决启动时显示不准确的问题，添加首次更新特殊处理
     */
    @JvmStatic
    fun updateAndGetDisplayPercent(actualPercent: Float): Float {
        val clampedPercent = actualPercent.coerceIn(0f, 100f)
        currentActualPercent = clampedPercent

        // 首次更新时，直接使用原始映射值，不经过滤波
        if (isFirstUpdate) {
            val rawDisplay = mapToDisplay(clampedPercent)
            filteredDisplayPercent = rawDisplay
            lastDisplayPercent = rawDisplay
            lastActualPercent = clampedPercent
            isFirstUpdate = false

            if (DEBUG) {
                Timber.d("首次更新: 实际=${clampedPercent}%, 显示=${rawDisplay}%")
            }
            return rawDisplay
        }

        return calculateDisplayPercent(clampedPercent)
    }

    /**
     * 计算显示电量
     */
    @JvmStatic
    fun calculateDisplayPercent(actualPercent: Float): Float {
        // 1. 原始映射
        val rawDisplay = mapToDisplay(actualPercent)

        if (DEBUG) {
            Timber.d("原始映射: 实际=${actualPercent}% -> 显示=${rawDisplay}%")
        }

        // 2. 应用滞后效应（修正：需要记录上次的实际电量）
        val withHysteresis = applyHysteresis(rawDisplay, actualPercent)

        // 3. 平滑处理
        val smoothed = applyLowPassFilter(withHysteresis)

        // 4. 边界限制
        return clamp(smoothed, 0f, MAX_PERCENT)
    }

    /**
     * 核心映射逻辑：实际电量 -> 显示电量
     */
    @JvmStatic
    private fun mapToDisplay(actualPercent: Float): Float {
        return when {
            // 实际电量0-20% -> 显示0%
            actualPercent <= LOCKED_PERCENT -> 0f

            // 实际电量100% -> 显示100%
            actualPercent >= MAX_PERCENT -> MAX_PERCENT

            // 实际电量90-100% -> 显示90-100% (线性)
            actualPercent >= HIGH_PERCENT_START -> {
                val highRangeActual = HIGH_PERCENT_START to MAX_PERCENT
                val highRangeDisplay = HIGH_PERCENT_START to MAX_PERCENT
                linearMap(actualPercent, highRangeActual, highRangeDisplay)
            }

            // 实际电量20-90% -> 显示0-90% (线性)
            else -> {
                val lowRangeActual = LOCKED_PERCENT to HIGH_PERCENT_START
                val lowRangeDisplay = 0f to HIGH_PERCENT_START
                linearMap(actualPercent, lowRangeActual, lowRangeDisplay)
            }
        }
    }

    /**
     * 线性映射辅助函数
     */
    @JvmStatic
    private fun linearMap(
        value: Float,
        fromRange: Pair<Float, Float>,
        toRange: Pair<Float, Float>
    ): Float {
        val (fromMin, fromMax) = fromRange
        val (toMin, toMax) = toRange

        if (fromMax - fromMin == 0f) return toMin

        val normalized = (value - fromMin) / (fromMax - fromMin)
        return toMin + normalized * (toMax - toMin)
    }

    /**
     * 应用滞后效应处理（修正版本）
     * 解决电量不减少的问题
     */
    @JvmStatic
    private fun applyHysteresis(currentPercent: Float, actualPercent: Float): Float {
        // 记录上次的实际电量
        val diff = currentPercent - lastDisplayPercent

        // 检查是否有充电状态变化（可选）
        val isCharging = isDeviceCharging()

        // 如果是放电状态且电量减少，即使变化很小也更新
        if (!isCharging && currentPercent < lastDisplayPercent) {
            // 放电状态下，只要电量减少就更新（即使变化很小）
            lastDisplayPercent = currentPercent
            lastActualPercent = actualPercent

            if (DEBUG && Math.abs(diff) > 0.1f) {
                Timber.d("放电状态: 电量减少 ${"%.1f".format(diff)}%，强制更新")
            }
            return currentPercent
        }

        // 充电状态或电量增加时，应用滞后阈值
        if (Math.abs(diff) < HYSTERESIS_THRESHOLD && lastActualPercent != 0f) {
            if (DEBUG) {
                Timber.d("滞后效应: 变化 ${"%.1f".format(diff)}% < 阈值 ${HYSTERESIS_THRESHOLD}%，保持原值")
            }
            return lastDisplayPercent
        }

        lastDisplayPercent = currentPercent
        lastActualPercent = actualPercent
        return currentPercent
    }

    /**
     * 检查设备是否在充电（需要外部实现）
     */
    @JvmStatic
    private fun isDeviceCharging(): Boolean {
        // 这里需要从您的应用状态中获取充电状态
        // 或者添加一个充电状态变量
        return false // 默认为放电状态
    }

    /**
     * 设置充电状态（外部调用）
     */
    @JvmStatic
    fun setChargingState(isCharging: Boolean) {
        if (DEBUG) {
            Timber.d("设置充电状态: $isCharging")
        }
        // 可以在这里存储充电状态，用于applyHysteresis方法
    }

    /**
     * 应用低通滤波器（改进版本）
     */
    @JvmStatic
    private fun applyLowPassFilter(currentPercent: Float): Float {
        // 如果当前值与滤波值差距过大，加速收敛
        val diff = Math.abs(currentPercent - filteredDisplayPercent)
        val effectiveAlpha = if (diff > 20f && !isFirstUpdate) {
            // 差距过大时使用更大的alpha加速收敛
            ALPHA * 3f
        } else {
            ALPHA
        }.coerceIn(ALPHA, 1.0f)

        filteredDisplayPercent =
            effectiveAlpha * currentPercent + (1 - effectiveAlpha) * filteredDisplayPercent

        if (DEBUG && effectiveAlpha != ALPHA) {
            Timber.d("加速收敛: alpha=$effectiveAlpha, 差距=$diff")
        }

        return filteredDisplayPercent
    }

    /**
     * 强制立即更新显示电量（跳过滤波和滞后）
     * 用于应用启动时或需要立即同步的场景
     */
    @JvmStatic
    fun forceUpdateDisplayPercent(actualPercent: Float): Float {
        val clampedPercent = actualPercent.coerceIn(0f, 100f)
        currentActualPercent = clampedPercent

        val rawDisplay = mapToDisplay(clampedPercent)
        filteredDisplayPercent = rawDisplay
        lastDisplayPercent = rawDisplay
        lastActualPercent = clampedPercent
        isFirstUpdate = false

        if (DEBUG) {
            Timber.d("强制更新: 实际=${clampedPercent}%, 显示=${rawDisplay}%")
        }

        return rawDisplay
    }

    /**
     * 获取当前显示电量
     */
    @JvmStatic
    fun getCurrentDisplayPercent(): Float = filteredDisplayPercent

    // 其他方法保持不变...
    @JvmStatic
    private fun clamp(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }
}