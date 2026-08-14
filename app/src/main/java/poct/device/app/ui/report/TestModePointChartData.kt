package poct.device.app.ui.report

import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.serialization.json.Json
import kotlin.math.abs
import poct.device.app.entity.CasePoint
import timber.log.Timber

data class TestModeSlopeRegion(
    val startIndex: Int,
    val endIndex: Int,
    val peakIndex: Int,
    val startPoint: CasePoint,
    val endPoint: CasePoint,
    val peakPoint: CasePoint,
    val area: Double,
)

object TestModePointChartData {
    // findSlopeRegions 可调入参的默认值
    private const val DEFAULT_FLAT_SLOPE_THRESHOLD = 0.2
    private const val DEFAULT_MIN_REGION_X_DISTANCE = 75.0
    private const val DEFAULT_MIN_PEAK_TROUGH_Y_DIFF = 50.0

    /** 噪声小波过滤：波幅低于最高波此比例的区域判为噪声丢弃。 */
    private const val NOISE_WAVE_AMP_RATIO = 0.1

    /** 波峰判定调参：近窗做局部最大检查，宽窗做左涨/右跌幅度检查（慢波在近窗内跌幅不足） */
    private object PeakDetect {
        const val NEAR_WINDOW_X = 20.0
        const val WIDE_WINDOW_X = 50.0

        /** 宽窗内左涨、右跌都要超过此幅度（与 minPeakTroughYDiff 取小） */
        const val MIN_RISE_DROP_Y = 20.0
    }

    /** 波形左右边界搜索调参 */
    private object BoundarySearch {
        /**
         * 区域跨度上限，从上升起点起算。缓慢的宽波（起点在波谷、缓升到峰再缓降到基线）整段跨度
         * 可达 ~480；上限过小会从起点截断，把下降段尾部砍掉。取 500 覆盖真机可见的最宽单波，
         * 又足以拦住越界失控的假区域（真实/黄金样本单波跨度均 <300，不受影响）。
         */
        const val MAX_REGION_X = 500.0

        /** 斜率取样窗口：跨若干点取斜率，抵抗逐点噪声（左边界回退与尾部平稳判定共用） */
        const val SLOPE_SAMPLE_X = 5.0

        /** 陡坡扫描窗口：肩部平台向左回看 / 双峰平台向前看共用 */
        const val STEEP_SCAN_X = 50.0

        /** 自适应平坦阈值 = 最陡坡度 × 此比例，大幅波形的缓坡段不算尾部 */
        const val ADAPTIVE_FLAT_RATIO = 0.1

        /** 前方"陡降"阈值 = 最陡坡度 × 此比例，需明显高于平坦阈值，谷底缓降不算陡降 */
        const val STEEP_DROP_RATIO = 0.3

        /**
         * 尾部"谷底回升即视为下一个波"判据的相对深度门槛：回升起算的谷底相对波峰的下探深度，
         * 需达到波幅（峰−起点）的此比例，才认定是真正的波间谷底。否则只是波内的肩部/次峰凹陷
         * （幅度 50 的绝对阈值对幅度数千的 ADC 波形过于敏感，主峰后一个浅凹再回升就会被当成新波，
         * 把整个下降段砍掉）。真正的波间谷底会回落到接近基线，浅肩凹陷仍高悬在峰附近。
         */
        const val NEW_WAVE_TROUGH_DEPTH_RATIO = 0.4

        /** 左边界回看：坡度超过此值视为上升仍在继续（肩部平台需要继续向左走） */
        const val STEEP_RISE_MIN_SLOPE = 1.0

        /** 幅度低于噪声底的波动不算"小波"，向左跳过时到此为止，防止爬过整段平坦谷底 */
        const val SMALL_WAVE_NOISE_FLOOR_Y = 10.0

        /**
         * 左边界回退的"净上升"判据窗口与阈值。逐点坡度判据（hasSteepRiseBehind）会把基线上
         * 的小纹波误当作"上升仍在继续"，导致左边界爬过整段波谷间基线锚死在很靠左处，再经
         * MAX_REGION_X 从错误起点截断，把真实波形的整个下降段砍掉。净上升判据看窗口两端的
         * 净高差：真实上升沿即便缓慢也有明确净上升，而基线纹波在窗口内净变化≈0，据此在波脚停住。
         */
        const val NET_RISE_SCAN_X = 20.0
        const val NET_RISE_MIN_Y = 5.0

        /**
         * 尾部"前方仍有陡降"判据的净下降窗口与阈值。与 hasSteepDropAhead 同理：逐点判据会把
         * 基线上的单点纹波（一两点内 -5~-7 的尖跌）误当作"波形仍在下降"，导致尾部越过整段平坦
         * 基线一直延伸到很靠后。净下降判据看窗口两端净高差：真正未结束的下降在窗口内有明确净
         * 跌幅，而平坦基线的纹波净变化≈0，据此在陡降转平缓的拐点停住。
         */
        const val NET_DROP_SCAN_X = 30.0
        const val NET_DROP_MIN_Y = 20.0

        /**
         * 尾部兜底继续判据的绝对净跌幅阈值：下降沿上短暂的肩部平台会误触发停止，但其后仍有大幅净
         * 下降通向真正波谷时应继续。取较大的绝对量（远大于缓降settle的净跌幅、又小于陡降沿），
         * 只对幅度数千的 ADC 真机波形生效，不影响幅度很小的单元测试合成波。
         */
        const val NET_DROP_CONTINUE_Y = 200.0

        /**
         * 左边界"越过凸起"判据：向左窗口内若有低于当前点超过 此比例×minPeakTroughYDiff 的地面，
         * 说明真正的波脚还在更左，当前只是踩在双峰/肩部的凸起上，应继续向左。比例取 3，足以跨过
         * 峰间浅凹旁的小凸起，又不会在真正波脚（左侧即基线，无明显更低地面）误判。
         */
        const val LOWER_GROUND_MIN_Y_RATIO = 3.0
    }

    fun parsePoints(workPoints: String): List<CasePoint> {
        if (workPoints.isBlank()) return emptyList()
        return try {
            Json.decodeFromString(workPoints)
        } catch (e: Exception) {
            Timber.tag("JSON").e(e.message ?: "JSON parser error")
            emptyList()
        }
    }

    fun toEntries(points: List<CasePoint>): List<FloatEntry> =
        points.map { FloatEntry(it.x.toFloat(), it.y.toFloat()) }

    fun toEntrySets(
        points: List<CasePoint>,
        regions: List<TestModeSlopeRegion>,
    ): List<List<FloatEntry>> {
        val regionLines =
            regions.map { region ->
                listOf(
                    FloatEntry(region.startPoint.x.toFloat(), region.startPoint.y.toFloat()),
                    FloatEntry(region.endPoint.x.toFloat(), region.endPoint.y.toFloat()),
                )
            }
        val peakPoints =
            regions.map { region ->
                listOf(FloatEntry(region.peakPoint.x.toFloat(), region.peakPoint.y.toFloat()))
            }
        val boundaryPoints =
            regions.map { region ->
                listOf(FloatEntry(region.startPoint.x.toFloat(), region.startPoint.y.toFloat()))
            }
        val endPoints =
            regions.map { region ->
                listOf(FloatEntry(region.endPoint.x.toFloat(), region.endPoint.y.toFloat()))
            }
        return listOf(toEntries(points)) + regionLines + peakPoints + boundaryPoints + endPoints
    }

    fun findSlopeRegions(
        points: List<CasePoint>,
        flatSlopeThreshold: Double = DEFAULT_FLAT_SLOPE_THRESHOLD,
        minRegionXDistance: Double = DEFAULT_MIN_REGION_X_DISTANCE,
        minPeakTroughYDiff: Double = DEFAULT_MIN_PEAK_TROUGH_Y_DIFF,
        filterNoiseWaves: Boolean = true,
    ): List<TestModeSlopeRegion> {
        if (points.size < 3) return emptyList()

        val regions = mutableListOf<TestModeSlopeRegion>()
        var index = 1

        while (index < points.lastIndex) {
            if (!isPeak(points, index, minPeakTroughYDiff) &&
                !isWideFlatTopOnset(points, index, minPeakTroughYDiff)
            ) {
                index++
                continue
            }

            val currentPeakFallback = index

            val riseStartIndex = findLeftFlatPoint(points, index, flatSlopeThreshold, minPeakTroughYDiff)
            var startIndex = skipSmallWavesOnLeft(points, riseStartIndex, minPeakTroughYDiff)

            var endIndex = findRightFlatPoint(points, index, startIndex, flatSlopeThreshold, minPeakTroughYDiff)

            if (points[endIndex].x - points[startIndex].x < minRegionXDistance) {
                index = maxOf(index + 1, currentPeakFallback + 1)
                continue
            }

            var peakIndex = findHighestPoint(points, startIndex, endIndex)
            var iterations = 0
            while (iterations < 10) {
                val recalculatedEndIndex = findRightFlatPoint(points, peakIndex, startIndex, flatSlopeThreshold, minPeakTroughYDiff)
                val recalculatedPeakIndex = findHighestPoint(points, startIndex, recalculatedEndIndex)

                if (recalculatedEndIndex == endIndex && recalculatedPeakIndex == peakIndex) {
                    break
                }
                endIndex = recalculatedEndIndex
                peakIndex = recalculatedPeakIndex
                iterations++
            }

            // 以最终波峰重新回推左边界：首次触发点可能落在噪声小包上，起点会被锚死在错误位置，
            // 进而导致跨度上限从错误起点截断出假波。此步只能向左延伸起点、绝不向右收缩：宽/圆
            // 顶波的波峰处近乎平坦（净上升≈0），从波峰回推会当场停在顶附近，反而把已找到的正确
            // 起点收窄到波形内部。取二者更靠左的一个即可两头兼顾。
            val reLeftStart = skipSmallWavesOnLeft(
                points,
                findLeftFlatPoint(points, peakIndex, flatSlopeThreshold, minPeakTroughYDiff),
                minPeakTroughYDiff,
            )
            startIndex = minOf(startIndex, reLeftStart)

            val maxEndX = points[startIndex].x + BoundarySearch.MAX_REGION_X
            if (points[endIndex].x > maxEndX) {
                val lastValid = (startIndex..endIndex).lastOrNull { points[it].x <= maxEndX }
                if (lastValid != null) {
                    endIndex = findRiseStartAtBoundary(points, lastValid)
                    peakIndex = findHighestPoint(points, startIndex, endIndex)
                }
            }

            val (adjStart, adjEnd, adjPeak) = adjustBoundsToKeepAboveLine(points, startIndex, endIndex, peakIndex)
            startIndex = adjStart
            endIndex = adjEnd
            peakIndex = adjPeak

            // 最终区域必须包含触发峰。否则说明触发点是基线上的小凸起，其向后搜索越过了真实波、
            // findHighestPoint 把波峰迁移到了更靠后的更高波上——若按 endIndex+1 前进，会把触发点与
            // 该波之间的真实波整段跳过（漏检）。此时只前进一步，让真实波的波峰自行触发。
            if (currentPeakFallback < startIndex || currentPeakFallback > endIndex) {
                index = currentPeakFallback + 1
                continue
            }

            // 与上一个已接收区域重叠：说明尾部的小凸起触发后向左越过波峰，把同一个波又检了一遍
            // （起点/波峰相同、末端不同）。丢弃这个重复区域，只前进一步。
            if (regions.isNotEmpty() && startIndex < regions.last().endIndex) {
                index = currentPeakFallback + 1
                continue
            }

            if (points[endIndex].x - points[startIndex].x < minRegionXDistance ||
                points[peakIndex].y - points[startIndex].y < minPeakTroughYDiff) {
                index = maxOf(index + 1, endIndex + 1)
                continue
            }

            regions.add(
                TestModeSlopeRegion(
                    startIndex = startIndex,
                    endIndex = endIndex,
                    peakIndex = peakIndex,
                    startPoint = points[startIndex],
                    endPoint = points[endIndex],
                    peakPoint = points[peakIndex],
                    area = calculateAreaBetweenCurveAndLine(points, startIndex, endIndex),
                )
            )
            index = maxOf(index + 1, endIndex + 1)
        }
        return if (filterNoiseWaves) dropNoiseWaves(regions) else regions
    }

    /** 区域的波幅：波峰相对自身基线（起点/终点较低者）的高度。 */
    private fun regionAmplitude(region: TestModeSlopeRegion): Double =
        region.peakPoint.y - minOf(region.startPoint.y, region.endPoint.y)

    /**
     * 检测完成后过滤噪声小波：相对最高波，波幅不足 NOISE_WAVE_AMP_RATIO 的判为噪声丢弃。
     * 用相对判据而非绝对阈值（后者 minPeakTroughYDiff 检测阶段已用），以适配不同基线/增益：
     * 通过了绝对下限、但相对主波极小的凸起才是噪声。最高波恒满足条件不会被清空。
     */
    private fun dropNoiseWaves(regions: List<TestModeSlopeRegion>): List<TestModeSlopeRegion> {
        if (regions.size <= 1) return regions
        val maxAmplitude = regions.maxOf(::regionAmplitude)
        if (maxAmplitude <= 0.0) return regions
        val threshold = maxAmplitude * NOISE_WAVE_AMP_RATIO
        return regions.filter { regionAmplitude(it) >= threshold }
    }

    private fun isPeak(points: List<CasePoint>, index: Int, minPeakTroughYDiff: Double): Boolean {
        if (index == 0 || index >= points.lastIndex) return false

        val threshold = minOf(PeakDetect.MIN_RISE_DROP_Y, minPeakTroughYDiff)
        val x = points[index].x
        val y = points[index].y

        // 左侧近窗局部最大 + 宽窗最低点（窗内无点时退化为相邻点）
        var leftMax = points[index - 1].y
        var leftWideMin = points[index - 1].y
        var left = index - 1
        while (left >= 0 && x - points[left].x <= PeakDetect.WIDE_WINDOW_X) {
            if (x - points[left].x <= PeakDetect.NEAR_WINDOW_X) {
                leftMax = maxOf(leftMax, points[left].y)
            }
            leftWideMin = minOf(leftWideMin, points[left].y)
            left--
        }

        // 右侧宽窗最低点
        var rightMin = points[index + 1].y
        var right = index + 1
        while (right < points.size && points[right].x - x <= PeakDetect.WIDE_WINDOW_X) {
            rightMin = minOf(rightMin, points[right].y)
            right++
        }

        // 局部最大 + 左侧有足够上涨 + 右侧有足够下降（宽窗才能识别缓慢升降的波）
        return y >= leftMax &&
                y - leftWideMin > threshold &&
                y - rightMin > threshold
    }

    /**
     * 宽平顶（削顶/饱和）波的波峰识别兜底。isPeak 要求波峰两侧都在 WIDE_WINDOW_X 内跌落，宽度
     * 超过 2×WIDE_WINDOW_X 的平顶平台里没有任何一点能同时看到两侧的跌落，于是整段波不触发、被漏检。
     * 这里只在 isPeak 覆盖不到的"宽平顶"上补一个触发点（平台前沿），不改动 isPeak，故窄平台/普通波
     * 的既有行为完全不变。
     */
    private fun isWideFlatTopOnset(points: List<CasePoint>, index: Int, minPeakTroughYDiff: Double): Boolean {
        if (index == 0 || index >= points.lastIndex) return false
        val y = points[index].y
        val threshold = minOf(PeakDetect.MIN_RISE_DROP_Y, minPeakTroughYDiff)

        // 平台前沿：上一点位于平台带之下（说明是刚升上来的前沿，只在此触发一次）
        if (y - points[index - 1].y <= threshold) return false

        // 向右量取平顶平台的宽度（带内视为同一平台）
        var runEnd = index
        while (runEnd < points.lastIndex && abs(points[runEnd + 1].y - y) <= threshold) {
            runEnd++
        }
        // 窄平台由 isPeak 处理，避免与其重复触发
        if (points[runEnd].x - points[index].x <= 2 * PeakDetect.WIDE_WINDOW_X) return false

        // 平台之后右侧必须跌落到平台带之下
        var afterPlateau = runEnd
        while (afterPlateau < points.lastIndex && y - points[afterPlateau].y <= threshold) {
            afterPlateau++
        }
        if (y - points[afterPlateau].y <= threshold) return false

        // 左侧宽窗内确有升入平台的跌落
        var leftMin = points[index - 1].y
        var left = index - 1
        while (left >= 0 && points[index].x - points[left].x <= PeakDetect.WIDE_WINDOW_X) {
            leftMin = minOf(leftMin, points[left].y)
            left--
        }
        return y - leftMin > threshold
    }

    private fun findHighestPoint(points: List<CasePoint>, startIndex: Int, endIndex: Int): Int {
        var maxIndex = startIndex
        for (index in startIndex..endIndex) {
            if (points[index].y > points[maxIndex].y) {
                maxIndex = index
            }
        }
        return maxIndex
    }

    private fun findLeftFlatPoint(
        points: List<CasePoint>,
        peakIndex: Int,
        flatSlopeThreshold: Double,
        minPeakTroughYDiff: Double,
    ): Int {
        var index = peakIndex
        var steepestRise = 0.0
        while (index > 0) {
            val slopeValue = sampledSlopeLeftOf(points, index)
            if (slopeValue > steepestRise) {
                steepestRise = slopeValue
            }
            val effectiveFlat = maxOf(flatSlopeThreshold, steepestRise * BoundarySearch.ADAPTIVE_FLAT_RATIO)
            // 上升仍在继续，需两个判据同时成立：逐点仍有陡升段，且窗口内确有净上升。
            // 只要有一个判据认为已进入基线（纹波无净上升）就在此停住，避免爬过波谷间基线。
            val stillRising =
                hasSteepRiseBehind(points, index, maxOf(BoundarySearch.STEEP_RISE_MIN_SLOPE, steepestRise * BoundarySearch.ADAPTIVE_FLAT_RATIO)) &&
                    hasNetRiseBehind(points, index)
            // 平顶（削顶/饱和）波：波峰是一段近似等值的平台（削顶还带纹波）。触发点落在宽平台内部时，
            // 净上升窗口整段都在平台上（净差≈0），会误判为"已到基线"而当场停住，使起点=波峰、波幅≈0
            // 被丢弃。仅当已从波峰下降超过一个顶部容差带（取 minPeakTroughYDiff，足以容纳削顶纹波、
            // 又小于任何合格波的波幅）后才允许平坦停止，从而先越过平台走到真正的上升沿再到波脚。
            // 双峰/肩部波：峰后（此处为峰左）有一个浅的峰间凹陷，凹陷紧邻的一侧又是个小凸起。
            // 在凹陷处 net-rise 窗口看到的相邻侧更高（净上升为负），会误判为已到波脚而停住，把起点
            // 卡在波形内部的凹陷上。若在扫描窗口内更靠左处仍有明显更低的地面（说明真正的上升沿/波脚
            // 还在更左），则说明当前只是踩在一个凸起上，应继续向左越过它。
            if (slopeValue <= effectiveFlat && !stillRising &&
                points[peakIndex].y - points[index].y > minPeakTroughYDiff &&
                !hasLowerGroundBehind(points, index, minPeakTroughYDiff)
            ) {
                break
            }
            index--
        }
        return index
    }

    /** 向左 STEEP_SCAN_X 窗口内是否仍有明显更低的地面（低于当前点超过 3×minPeakTroughYDiff）。 */
    private fun hasLowerGroundBehind(points: List<CasePoint>, fromIndex: Int, minPeakTroughYDiff: Double): Boolean {
        val limitX = points[fromIndex].x - BoundarySearch.STEEP_SCAN_X
        var minBehind = points[fromIndex].y
        var i = fromIndex - 1
        while (i >= 0 && points[i].x >= limitX) {
            minBehind = minOf(minBehind, points[i].y)
            i--
        }
        return points[fromIndex].y - minBehind > BoundarySearch.LOWER_GROUND_MIN_Y_RATIO * minPeakTroughYDiff
    }

    /** 从 fromIndex 向左取 NET_RISE_SCAN_X 窗口，窗口两端净高差超过阈值才算"仍在上升沿" */
    private fun hasNetRiseBehind(points: List<CasePoint>, fromIndex: Int): Boolean {
        val limitX = points[fromIndex].x - BoundarySearch.NET_RISE_SCAN_X
        var back = fromIndex
        while (back > 0 && points[back - 1].x >= limitX) {
            back--
        }
        return points[fromIndex].y - points[back].y > BoundarySearch.NET_RISE_MIN_Y
    }

    /** 从 index 向左跨取样窗口的斜率，抵抗逐点噪声（窗口内无点时退化为相邻点） */
    private fun sampledSlopeLeftOf(points: List<CasePoint>, index: Int): Double {
        var back = index - 1
        while (back > 0 && points[index].x - points[back - 1].x <= BoundarySearch.SLOPE_SAMPLE_X) {
            back--
        }
        return slope(points[back], points[index])
    }

    /** 从 index 向右跨取样窗口的斜率，抵抗逐点噪声（窗口内无点时退化为相邻点） */
    private fun sampledSlopeRightOf(points: List<CasePoint>, index: Int): Double {
        var forward = index + 1
        while (forward < points.lastIndex && points[forward + 1].x - points[index].x <= BoundarySearch.SLOPE_SAMPLE_X) {
            forward++
        }
        return slope(points[index], points[forward])
    }

    /** 左侧回看窗口内是否仍有连续陡升段（波形肩部平台需要继续向左回退） */
    private fun hasSteepRiseBehind(points: List<CasePoint>, fromIndex: Int, steepSlopeThreshold: Double): Boolean {
        val limitX = points[fromIndex].x - BoundarySearch.STEEP_SCAN_X
        var i = fromIndex
        var consecutiveSteepCount = 0
        while (i > 0 && points[i].x > limitX) {
            if (slope(points[i - 1], points[i]) > steepSlopeThreshold) {
                consecutiveSteepCount++
                if (consecutiveSteepCount >= 2) return true
            } else {
                consecutiveSteepCount = 0
            }
            i--
        }
        return false
    }

    private fun skipSmallWavesOnLeft(points: List<CasePoint>, startIndex: Int, minPeakTroughYDiff: Double): Int {
        var start = startIndex
        while (start > 0) {
            var peakIndex = start
            while (peakIndex > 0 && points[peakIndex - 1].y >= points[peakIndex].y) {
                peakIndex--
            }
            if (peakIndex == start) break

            var troughIndex = peakIndex
            while (troughIndex > 0 && points[troughIndex - 1].y <= points[troughIndex].y) {
                troughIndex--
            }
            val amplitude = points[peakIndex].y - minOf(points[start].y, points[troughIndex].y)
            if (amplitude >= minPeakTroughYDiff) break
            // 低于噪声底的波动不是"小波"，继续向左只会爬过整段平坦谷底
            if (amplitude < minOf(BoundarySearch.SMALL_WAVE_NOISE_FLOOR_Y, minPeakTroughYDiff / 2)) break
            start = troughIndex
        }
        return start
    }

    private fun findRightFlatPoint(
        points: List<CasePoint>,
        peakIndex: Int,
        startIndex: Int,
        flatSlopeThreshold: Double,
        minPeakTroughYDiff: Double,
    ): Int {
        val peakY = points[peakIndex].y
        // 波幅（峰−起点），用于把"波间谷底"与"波内浅凹"区分开
        val amplitude = peakY - points[startIndex].y
        // 下降 5%，但不超过波形最小高度：ADC 基线较高时 5% 绝对值可能永远达不到
        val dropThreshold = minOf(peakY * 0.05, minPeakTroughYDiff)

        var declineStart = -1
        var index = peakIndex + 1

        while (index < points.lastIndex) {
            val drop = peakY - points[index].y
            val slopeValue = slope(points[index - 1], points[index])

            if (drop > dropThreshold && slopeValue < -flatSlopeThreshold) {
                declineStart = index
                break
            }
            index++
        }

        if (declineStart < 0) {
            return points.lastIndex
        }

        // 继续向后寻找真正尾部
        var tail = declineStart
        var stableCount = 0
        var minIndex = declineStart
        var steepestDrop = 0.0

        while (tail < points.lastIndex - 1) {
            if (points[tail].y < points[minIndex].y) {
                minIndex = tail
            }
            // 从谷底重新上涨超过阈值说明下一个波开始了，尾部就是谷底。但仅当谷底确实回落到接近
            // 基线（相对波幅下探足够深）才算波间谷底；否则只是主峰后的肩部/次峰浅凹，继续向后走，
            // 否则会把双峰波在第一个峰间凹陷处截断，砍掉整个下降段。
            if (points[tail].y - points[minIndex].y > minPeakTroughYDiff) {
                val troughDepth = peakY - points[minIndex].y
                if (amplitude <= 0.0 || troughDepth >= amplitude * BoundarySearch.NEW_WAVE_TROUGH_DEPTH_RATIO) {
                    return minIndex
                }
            }

            // 斜率在前向窗口上取样，逐点噪声不会打断平稳段计数
            val nextSlope = sampledSlopeRightOf(points, tail)
            if (nextSlope < steepestDrop) {
                steepestDrop = nextSlope
            }
            // 平坦阈值随最陡下降坡度自适应，大波形的缓坡段不算尾部
            val effectiveFlat = maxOf(flatSlopeThreshold, abs(steepestDrop) * BoundarySearch.ADAPTIVE_FLAT_RATIO)

            // 接近水平
            if (abs(nextSlope) < effectiveFlat) {
                stableCount++
                if (stableCount >= 10) {
                    // 双峰间的平台：前方还有明显陡降说明波形未结束，继续向后走；
                    // 陡降阈值远高于平坦阈值，谷底的缓慢下坡不算。再叠加"净下降"判据，
                    // 否则平坦基线上的单点纹波尖跌会被当成陡降，把尾部一直拖到很靠后。
                    // 另一路兜底：下降沿上偶有短暂的肩部平台会误触发停止，但其后仍有大幅净下降通向真正
                    // 波谷（前方净跌幅超过 NET_DROP_CONTINUE_Y 的绝对量），此时也继续，避免在肩部截断。
                    val steepThreshold = maxOf(flatSlopeThreshold, abs(steepestDrop) * BoundarySearch.STEEP_DROP_RATIO)
                    val stillDescending =
                        (hasSteepDropAhead(points, tail, steepThreshold) && hasNetDropAhead(points, tail)) ||
                            netDropAheadAmount(points, tail) > BoundarySearch.NET_DROP_CONTINUE_Y
                    if (stillDescending) {
                        stableCount = 0
                    } else {
                        return tail
                    }
                }
            } else {
                stableCount = 0
            }
            tail++
        }

        return tail
    }

    /** 从 fromIndex 向右取 NET_DROP_SCAN_X 窗口的净跌幅（正值表示仍在下降）。 */
    private fun netDropAheadAmount(points: List<CasePoint>, fromIndex: Int): Double {
        val limitX = points[fromIndex].x + BoundarySearch.NET_DROP_SCAN_X
        var ahead = fromIndex
        while (ahead < points.lastIndex && points[ahead + 1].x <= limitX) {
            ahead++
        }
        return points[fromIndex].y - points[ahead].y
    }

    /** 从 fromIndex 向右取 NET_DROP_SCAN_X 窗口，窗口两端净跌幅超过阈值才算"前方仍在真正下降" */
    private fun hasNetDropAhead(points: List<CasePoint>, fromIndex: Int): Boolean {
        return netDropAheadAmount(points, fromIndex) > BoundarySearch.NET_DROP_MIN_Y
    }

    /** 前向扫描窗口内是否仍有连续陡降段（双峰间的平台说明波形未结束） */
    private fun hasSteepDropAhead(points: List<CasePoint>, fromIndex: Int, steepSlopeThreshold: Double): Boolean {
        val limitX = points[fromIndex].x + BoundarySearch.STEEP_SCAN_X
        var i = fromIndex
        var consecutiveSteepCount = 0
        while (i < points.lastIndex && points[i].x < limitX) {
            if (slope(points[i], points[i + 1]) < -steepSlopeThreshold) {
                consecutiveSteepCount++
                if (consecutiveSteepCount >= 2) return true
            } else {
                consecutiveSteepCount = 0
            }
            i++
        }
        return false
    }

    private fun adjustBoundsToKeepAboveLine(
        points: List<CasePoint>,
        startIndex: Int,
        endIndex: Int,
        peakIndex: Int,
    ): Triple<Int, Int, Int> {
        var start = startIndex
        var end = endIndex
        var peak = peakIndex

        val visitedStarts = mutableSetOf<Int>()
        val visitedEnds = mutableSetOf<Int>()

        for (k in 0 until points.size) {
            val violation = findFirstViolation(points, start, end)
            if (violation == -1) return Triple(start, end, peak)

            if (visitedStarts.contains(start) && visitedEnds.contains(end)) {
                break
            }
            visitedStarts.add(start)
            visitedEnds.add(end)

            if (violation <= peak) {
                if (violation >= peak) return Triple(start, end, peak)
                start = violation
            } else {
                if (violation <= start) return Triple(start, end, peak)
                end = violation
            }
            peak = findHighestPoint(points, start, end)
        }
        return Triple(start, end, peak)
    }

    private fun findFirstViolation(points: List<CasePoint>, startIndex: Int, endIndex: Int): Int {
        val startPoint = points[startIndex]
        val endPoint = points[endIndex]
        val dx = endPoint.x - startPoint.x
        if (dx <= 0.0) return -1
        for (i in startIndex + 1 until endIndex) {
            val lineY = interpolateLineY(startPoint, endPoint, points[i].x, dx)
            if (points[i].y < lineY - 1e-5) return i
        }
        return -1
    }

    private fun findRiseStartAtBoundary(points: List<CasePoint>, boundaryIndex: Int): Int {
        var i = boundaryIndex
        while (i > 0 && slope(points[i - 1], points[i]) > 0.0) {
            i--
        }
        return i
    }

    private fun slope(startPoint: CasePoint, endPoint: CasePoint): Double {
        val dx = endPoint.x - startPoint.x
        if (dx <= 0.0) return 0.0
        return (endPoint.y - startPoint.y) / dx
    }

    internal fun calculateAreaBetweenCurveAndLine(points: List<CasePoint>, startIndex: Int, endIndex: Int): Double {
        if (endIndex <= startIndex) return 0.0
        val startPoint = points[startIndex]
        val endPoint = points[endIndex]
        val dx = endPoint.x - startPoint.x
        if (dx == 0.0) return 0.0

        var area = 0.0
        for (index in startIndex until endIndex) {
            val leftPoint = points[index]
            val rightPoint = points[index + 1]
            val width = rightPoint.x - leftPoint.x
            if (width <= 0.0) continue

            val leftDiff = leftPoint.y - interpolateLineY(startPoint, endPoint, leftPoint.x, dx)
            val rightDiff = rightPoint.y - interpolateLineY(startPoint, endPoint, rightPoint.x, dx)
            area += calculateSegmentArea(leftDiff, rightDiff, width)
        }
        return area
    }

    private fun calculateSegmentArea(leftDiff: Double, rightDiff: Double, width: Double): Double {
        if (leftDiff == 0.0 || rightDiff == 0.0 || leftDiff * rightDiff > 0.0) {
            return (abs(leftDiff) + abs(rightDiff)) * width / 2.0
        }
        val leftAbsDiff = abs(leftDiff)
        val rightAbsDiff = abs(rightDiff)
        val totalDiff = leftAbsDiff + rightAbsDiff
        if (totalDiff == 0.0) return 0.0

        val leftWidth = width * leftAbsDiff / totalDiff
        val rightWidth = width - leftWidth
        return leftAbsDiff * leftWidth / 2.0 + rightAbsDiff * rightWidth / 2.0
    }

    private fun interpolateLineY(startPoint: CasePoint, endPoint: CasePoint, x: Double, dx: Double): Double {
        val ratio = (x - startPoint.x) / dx
        return startPoint.y + (endPoint.y - startPoint.y) * ratio
    }
}