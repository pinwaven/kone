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
    private const val MIN_PEAK_START_HEIGHT_DIFF = 5.0

    fun parsePoints(workPoints: String): List<CasePoint> {
        if (workPoints.isBlank()) {
            return emptyList()
        }
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
        return listOf(toEntries(points)) + regionLines + peakPoints + boundaryPoints
    }

    fun findSlopeRegions(
            points: List<CasePoint>,
            flatSlopeThreshold: Double = DEFAULT_FLAT_SLOPE_THRESHOLD,
            minRegionXDistance: Double = MIN_REGION_X_DISTANCE,
            minPeakToEndXDistance: Double = MIN_PEAK_TO_END_X_DISTANCE,
    ): List<TestModeSlopeRegion> {
        if (points.size < 3) {
            return emptyList()
        }

        val regions = mutableListOf<TestModeSlopeRegion>()
        var index = 1
        while (index < points.lastIndex) {
            if (!isPeak(points, index)) {
                index++
                continue
            }

            val startIndex = findLeftFlatPoint(points, index, flatSlopeThreshold)
            var endIndex =
                    findRightFlatPoint(
                            points = points,
                            peakIndex = index,
                            flatSlopeThreshold = flatSlopeThreshold,
                            minPeakToEndXDistance = minPeakToEndXDistance,
                    )
            if (points[endIndex].x - points[startIndex].x < minRegionXDistance) {
                index++
                continue
            }
            var peakIndex = findHighestPoint(points, startIndex, endIndex)
            while (true) {
                val recalculatedEndIndex =
                        findRightFlatPoint(
                                points = points,
                                peakIndex = peakIndex,
                                flatSlopeThreshold = flatSlopeThreshold,
                                minPeakToEndXDistance = minPeakToEndXDistance,
                        )
                val recalculatedPeakIndex =
                        findHighestPoint(points, startIndex, recalculatedEndIndex)
                if (recalculatedEndIndex == endIndex && recalculatedPeakIndex == peakIndex) {
                    break
                }
                endIndex = recalculatedEndIndex
                peakIndex = recalculatedPeakIndex
            }
            if (points[peakIndex].y - points[startIndex].y < MIN_PEAK_START_HEIGHT_DIFF) {
                index++
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
            index = endIndex + 1
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

    private fun findLeftFlatPoint(
            points: List<CasePoint>,
            peakIndex: Int,
            flatSlopeThreshold: Double,
    ): Int {
        var index = peakIndex
        while (index > 0 && slope(points[index - 1], points[index]) > flatSlopeThreshold) {
            index--
        }
        return index
    }

    private fun findRightFlatPoint(
            points: List<CasePoint>,
            peakIndex: Int,
            flatSlopeThreshold: Double,
            minPeakToEndXDistance: Double,
    ): Int {
        var index = peakIndex
        var hasDownwardSlope = false
        while (index < points.lastIndex) {
            val currentSlope = slope(points[index], points[index + 1])
            if (currentSlope < -flatSlopeThreshold) {
                hasDownwardSlope = true
                index++
                continue
            }
            val peakToEndXDistance = points[index].x - points[peakIndex].x
            if (hasDownwardSlope &&
                            peakToEndXDistance >= minPeakToEndXDistance
            ) {
                return index
            }
            index++
        }
        return index
    }

    private fun slope(startPoint: CasePoint, endPoint: CasePoint): Double {
        val dx = endPoint.x - startPoint.x
        if (dx <= 0.0) {
            return 0.0
        }
        return (endPoint.y - startPoint.y) / dx
    }

    internal fun calculateAreaBetweenCurveAndLine(
            points: List<CasePoint>,
            startIndex: Int,
            endIndex: Int,
    ): Double {
        if (endIndex <= startIndex) {
            return 0.0
        }

        val startPoint = points[startIndex]
        val endPoint = points[endIndex]
        val dx = endPoint.x - startPoint.x
        if (dx == 0.0) {
            return 0.0
        }

        var area = 0.0
        for (index in startIndex until endIndex) {
            val leftPoint = points[index]
            val rightPoint = points[index + 1]
            val width = rightPoint.x - leftPoint.x
            if (width <= 0.0) {
                continue
            }

            val leftDiff = leftPoint.y - interpolateLineY(startPoint, endPoint, leftPoint.x, dx)
            val rightDiff = rightPoint.y - interpolateLineY(startPoint, endPoint, rightPoint.x, dx)
            area += calculateSegmentArea(leftDiff, rightDiff, width)
        }
        return area
    }

    private fun calculateSegmentArea(
            leftDiff: Double,
            rightDiff: Double,
            width: Double,
    ): Double {
        if (leftDiff == 0.0 || rightDiff == 0.0 || leftDiff * rightDiff > 0.0) {
            return (abs(leftDiff) + abs(rightDiff)) * width / 2.0
        }

        val leftAbsDiff = abs(leftDiff)
        val rightAbsDiff = abs(rightDiff)
        val totalDiff = leftAbsDiff + rightAbsDiff
        if (totalDiff == 0.0) {
            return 0.0
        }

        val leftWidth = width * leftAbsDiff / totalDiff
        val rightWidth = width - leftWidth
        return leftAbsDiff * leftWidth / 2.0 + rightAbsDiff * rightWidth / 2.0
    }

    private fun interpolateLineY(
            startPoint: CasePoint,
            endPoint: CasePoint,
            x: Double,
            dx: Double,
    ): Double {
        val ratio = (x - startPoint.x) / dx
        return startPoint.y + (endPoint.y - startPoint.y) * ratio
    }
}
