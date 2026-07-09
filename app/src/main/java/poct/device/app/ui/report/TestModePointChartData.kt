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
    private const val DEFAULT_FLAT_SLOPE_THRESHOLD = 0.2
    private const val MIN_REGION_X_DISTANCE = 75.0
    private const val MIN_PEAK_TO_END_X_DISTANCE = 50.0
    private const val MIN_PEAK_TROUGH_Y_DIFF = 50.0
    // 缩短向后看的视野，让长尾判定更灵敏，防止半山腰误判
    private const val STEEP_DROP_LOOKAHEAD_X_DISTANCE = 50.0
    // 【核心修复】：放开跨度上限到 400，完美包裹 260 ~ 633 跨度达 373 点的超级大波
    private const val MAX_REGION_X_DISTANCE = 400.0

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
        minRegionXDistance: Double = MIN_REGION_X_DISTANCE,
        minPeakToEndXDistance: Double = MIN_PEAK_TO_END_X_DISTANCE,
        minPeakTroughYDiff: Double = MIN_PEAK_TROUGH_Y_DIFF,
    ): List<TestModeSlopeRegion> {
        if (points.size < 3) return emptyList()

        val regions = mutableListOf<TestModeSlopeRegion>()
        var index = 1

        while (index < points.lastIndex) {
            if (!isPeak(points, index)) {
                index++
                continue
            }

            val currentPeakFallback = index

            val riseStartIndex = findLeftFlatPoint(points, index, flatSlopeThreshold)
            var startIndex = skipSmallWavesOnLeft(points, riseStartIndex, minPeakTroughYDiff)

            val originalEndIndex = findRightFlatPoint(points, index, flatSlopeThreshold, minPeakToEndXDistance)
            var endIndex = originalEndIndex

            if (points[endIndex].x - points[startIndex].x < minRegionXDistance) {
                index = maxOf(index + 1, currentPeakFallback + 1)
                continue
            }

            var peakIndex = findHighestPoint(points, startIndex, endIndex)
            var iterations = 0
            while (iterations < 10) {
                val recalculatedEndIndex = findRightFlatPoint(points, peakIndex, flatSlopeThreshold, minPeakToEndXDistance)
                val recalculatedPeakIndex = findHighestPoint(points, startIndex, recalculatedEndIndex)

                if (endIndex - recalculatedEndIndex > 20) {
                    break
                }

                if (recalculatedEndIndex == endIndex && recalculatedPeakIndex == peakIndex) {
                    break
                }
                endIndex = recalculatedEndIndex
                peakIndex = recalculatedPeakIndex
                iterations++
            }

            val maxEndX = points[startIndex].x + MAX_REGION_X_DISTANCE
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

    private fun isPeak(points: List<CasePoint>, index: Int): Boolean =
        points[index].y > points[index - 1].y && points[index].y >= points[index + 1].y

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
        while (index > 0 && slope(points[index - 1], points[index]) > flatSlopeThreshold) {
            index--
        }
        return index
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
            start = troughIndex
        }
        return start
    }

    private fun findRightFlatPoint(
        points: List<CasePoint>,
        peakIndex: Int,
        flatSlopeThreshold: Double,
        minPeakToEndXDistance: Double,
    ): Int {
        var index = peakIndex
        var hasDownwardSlope = false
        val peakY = points[peakIndex].y
        val requiredDrop = 50.0
        while (index < points.lastIndex) {
            val currentSlope = slope(points[index], points[index + 1])
            if (currentSlope < -flatSlopeThreshold) {
                hasDownwardSlope = true
                index++
                continue
            }
            val peakToEndXDistance = points[index].x - points[peakIndex].x
            val currentDrop = peakY - points[index].y

            if (hasDownwardSlope && peakToEndXDistance >= minPeakToEndXDistance && currentDrop >= requiredDrop) {
                if (!hasSteepDropAhead(points, index, flatSlopeThreshold)) {
                    return findMinInFlatTail(points, index, flatSlopeThreshold)
                }
            }
            index++
        }
        return index
    }

    private fun findMinInFlatTail(points: List<CasePoint>, fromIndex: Int, flatSlopeThreshold: Double): Int {
        var minIndex = fromIndex
        var i = fromIndex
        var flatCount = 0
        while (i < points.lastIndex) {
            val currentSlope = slope(points[i], points[i + 1])

            if (currentSlope > flatSlopeThreshold * 1.5) {
                flatCount++
                if (flatCount > 3) break
            } else {
                flatCount = 0
            }

            if (points[i].y < points[minIndex].y) {
                minIndex = i
            }
            i++
        }
        return minIndex
    }

    private fun hasSteepDropAhead(points: List<CasePoint>, fromIndex: Int, flatSlopeThreshold: Double): Boolean {
        val limitX = points[fromIndex].x + STEEP_DROP_LOOKAHEAD_X_DISTANCE
        var i = fromIndex
        var consecutiveSteepCount = 0
        while (i < points.lastIndex && points[i].x < limitX) {
            if (slope(points[i], points[i + 1]) < -flatSlopeThreshold) {
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