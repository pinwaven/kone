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

    /** 波峰判定调参：近窗做局部最大检查，宽窗做左涨/右跌幅度检查（慢波在近窗内跌幅不足） */
    private object PeakDetect {
        const val NEAR_WINDOW_X = 20.0
        const val WIDE_WINDOW_X = 50.0

        /** 宽窗内左涨、右跌都要超过此幅度（与 minPeakTroughYDiff 取小） */
        const val MIN_RISE_DROP_Y = 20.0
    }

    /** 波形左右边界搜索调参 */
    private object BoundarySearch {
        /** 区域跨度上限，从上升起点起算 */
        const val MAX_REGION_X = 400.0

        /** 斜率取样窗口：跨若干点取斜率，抵抗逐点噪声（左边界回退与尾部平稳判定共用） */
        const val SLOPE_SAMPLE_X = 5.0

        /** 陡坡扫描窗口：肩部平台向左回看 / 双峰平台向前看共用 */
        const val STEEP_SCAN_X = 50.0

        /** 自适应平坦阈值 = 最陡坡度 × 此比例，大幅波形的缓坡段不算尾部 */
        const val ADAPTIVE_FLAT_RATIO = 0.1

        /** 前方"陡降"阈值 = 最陡坡度 × 此比例，需明显高于平坦阈值，谷底缓降不算陡降 */
        const val STEEP_DROP_RATIO = 0.3

        /** 左边界回看：坡度超过此值视为上升仍在继续（肩部平台需要继续向左走） */
        const val STEEP_RISE_MIN_SLOPE = 1.0

        /** 幅度低于噪声底的波动不算"小波"，向左跳过时到此为止，防止爬过整段平坦谷底 */
        const val SMALL_WAVE_NOISE_FLOOR_Y = 10.0
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
    ): List<TestModeSlopeRegion> {
        if (points.size < 3) return emptyList()

        val regions = mutableListOf<TestModeSlopeRegion>()
        var index = 1

        while (index < points.lastIndex) {
            if (!isPeak(points, index, minPeakTroughYDiff)) {
                index++
                continue
            }

            val currentPeakFallback = index

            val riseStartIndex = findLeftFlatPoint(points, index, flatSlopeThreshold)
            var startIndex = skipSmallWavesOnLeft(points, riseStartIndex, minPeakTroughYDiff)

            var endIndex = findRightFlatPoint(points, index, flatSlopeThreshold, minPeakTroughYDiff)

            if (points[endIndex].x - points[startIndex].x < minRegionXDistance) {
                index = maxOf(index + 1, currentPeakFallback + 1)
                continue
            }

            var peakIndex = findHighestPoint(points, startIndex, endIndex)
            var iterations = 0
            while (iterations < 10) {
                val recalculatedEndIndex = findRightFlatPoint(points, peakIndex, flatSlopeThreshold, minPeakTroughYDiff)
                val recalculatedPeakIndex = findHighestPoint(points, startIndex, recalculatedEndIndex)

                if (recalculatedEndIndex == endIndex && recalculatedPeakIndex == peakIndex) {
                    break
                }
                endIndex = recalculatedEndIndex
                peakIndex = recalculatedPeakIndex
                iterations++
            }

            // 以最终波峰重新回推左边界：首次触发点可能落在噪声小包上，起点会被锚死在错误位置，
            // 进而导致跨度上限从错误起点截断出假波
            startIndex = skipSmallWavesOnLeft(
                points,
                findLeftFlatPoint(points, peakIndex, flatSlopeThreshold),
                minPeakTroughYDiff,
            )

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
        return regions
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

    private fun findHighestPoint(points: List<CasePoint>, startIndex: Int, endIndex: Int): Int {
        var maxIndex = startIndex
        for (index in startIndex..endIndex) {
            if (points[index].y > points[maxIndex].y) {
                maxIndex = index
            }
        }
        return maxIndex
    }

    private fun findLeftFlatPoint(points: List<CasePoint>, peakIndex: Int, flatSlopeThreshold: Double): Int {
        var index = peakIndex
        var steepestRise = 0.0
        while (index > 0) {
            val slopeValue = sampledSlopeLeftOf(points, index)
            if (slopeValue > steepestRise) {
                steepestRise = slopeValue
            }
            val effectiveFlat = maxOf(flatSlopeThreshold, steepestRise * BoundarySearch.ADAPTIVE_FLAT_RATIO)
            if (slopeValue <= effectiveFlat &&
                !hasSteepRiseBehind(points, index, maxOf(BoundarySearch.STEEP_RISE_MIN_SLOPE, steepestRise * BoundarySearch.ADAPTIVE_FLAT_RATIO))
            ) {
                break
            }
            index--
        }
        return index
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
        flatSlopeThreshold: Double,
        minPeakTroughYDiff: Double,
    ): Int {
        val peakY = points[peakIndex].y
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
            // 从谷底重新上涨超过阈值说明下一个波开始了，尾部就是谷底
            if (points[tail].y - points[minIndex].y > minPeakTroughYDiff) {
                return minIndex
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
                    // 陡降阈值远高于平坦阈值，谷底的缓慢下坡不算
                    val steepThreshold = maxOf(flatSlopeThreshold, abs(steepestDrop) * BoundarySearch.STEEP_DROP_RATIO)
                    if (hasSteepDropAhead(points, tail, steepThreshold)) {
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