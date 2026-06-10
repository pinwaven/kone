package poct.device.app.ui.report

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import poct.device.app.entity.CasePoint

class TestModePointChartDataTest {
    @Test
    fun parsePointsUsesWorkPointsOnly() {
        val points =
                listOf(
                        CasePoint(1026.0, 1069.0),
                        CasePoint(1027.0, 1064.0),
                )
        val result = TestModePointChartData.parsePoints(Json.encodeToString(points))

        assertEquals(2, result.size)
        assertEquals(1026.0, result[0].x, 0.0)
        assertEquals(1069.0, result[0].y, 0.0)
        assertEquals(1027.0, result[1].x, 0.0)
        assertEquals(1064.0, result[1].y, 0.0)
    }

    @Test
    fun parsePointsReturnsEmptyListForInvalidJson() {
        val result = TestModePointChartData.parsePoints("not-json")

        assertTrue(result.isEmpty())
    }

    @Test
    fun toEntriesKeepsPointXAsHorizontalAxisAndPointYAsVerticalAxis() {
        val entries =
                TestModePointChartData.toEntries(
                        listOf(
                                CasePoint(1026.0, 1069.0),
                                CasePoint(1027.0, 1064.0),
                        )
                )

        assertEquals(1026f, entries[0].x, 0.0f)
        assertEquals(1069f, entries[0].y, 0.0f)
        assertEquals(1027f, entries[1].x, 0.0f)
        assertEquals(1064f, entries[1].y, 0.0f)
    }

    @Test
    fun toEntrySetsAddsRegionLinesPeakPointsAndStartPointsOnly() {
        val points =
                listOf(
                        CasePoint(0.0, 0.0),
                        CasePoint(20.0, 1.0),
                        CasePoint(40.0, 11.0),
                        CasePoint(60.0, 21.0),
                        CasePoint(80.0, 31.0),
                        CasePoint(100.0, 21.0),
                        CasePoint(120.0, 11.0),
                        CasePoint(140.0, 1.0),
                        CasePoint(160.0, 0.0),
                )
        val region = TestModePointChartData.findSlopeRegions(points).first()

        val entrySets = TestModePointChartData.toEntrySets(points, listOf(region))

        assertEquals(4, entrySets.size)
        assertEquals(points.size, entrySets[0].size)
        assertEquals(2, entrySets[1].size)
        assertEquals(1, entrySets[2].size)
        assertEquals(1, entrySets[3].size)
        assertEquals(80f, entrySets[2][0].x, 0.0f)
        assertEquals(31f, entrySets[2][0].y, 0.0f)
        assertEquals(20f, entrySets[3][0].x, 0.0f)
        assertEquals(1f, entrySets[3][0].y, 0.0f)
    }

    @Test
    fun calculateAreaBetweenCurveAndLineSplitsSegmentsAtLineCrossings() {
        val area =
                TestModePointChartData.calculateAreaBetweenCurveAndLine(
                        points =
                                listOf(
                                        CasePoint(0.0, 0.0),
                                        CasePoint(10.0, 10.0),
                                        CasePoint(20.0, -10.0),
                                        CasePoint(30.0, 0.0),
                                ),
                        startIndex = 0,
                        endIndex = 3,
                )

        assertEquals(150.0, area, 0.0001)
    }

    @Test
    fun findSlopeRegionsUsesPeakAndFlatSlopeBoundaries() {
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 0.0),
                                CasePoint(20.0, 1.0),
                                CasePoint(40.0, 11.0),
                                CasePoint(60.0, 21.0),
                                CasePoint(80.0, 31.0),
                                CasePoint(100.0, 21.0),
                                CasePoint(120.0, 11.0),
                                CasePoint(140.0, 1.0),
                                CasePoint(160.0, 0.0),
                        )
                )

        assertEquals(1, regions.size)
        assertEquals(1, regions[0].startIndex)
        assertEquals(7, regions[0].endIndex)
        assertEquals(1800.0, regions[0].area, 0.0001)
    }

    @Test
    fun findSlopeRegionsIgnoresPeaksWithSmallRiseFromStart() {
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 10.0),
                                CasePoint(20.0, 10.2),
                                CasePoint(40.0, 10.8),
                                CasePoint(60.0, 11.4),
                                CasePoint(80.0, 11.7),
                                CasePoint(100.0, 11.1),
                                CasePoint(120.0, 10.5),
                                CasePoint(140.0, 10.2),
                                CasePoint(160.0, 10.1),
                        )
                )

        assertTrue(regions.isEmpty())
    }

    @Test
    fun findSlopeRegionsUsesHighestPointInRisingRegionAsPeak() {
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 0.0),
                                CasePoint(20.0, 1.0),
                                CasePoint(40.0, 11.0),
                                CasePoint(60.0, 21.0),
                                CasePoint(80.0, 19.0),
                                CasePoint(100.0, 29.0),
                                CasePoint(120.0, 39.0),
                                CasePoint(140.0, 30.0),
                                CasePoint(160.0, 20.0),
                                CasePoint(180.0, 10.0),
                                CasePoint(200.0, 9.0),
                        )
                )

        assertEquals(1, regions.size)
        assertEquals(6, regions[0].peakIndex)
        assertEquals(120.0, regions[0].peakPoint.x, 0.0)
        assertEquals(39.0, regions[0].peakPoint.y, 0.0)
    }

    @Test
    fun findSlopeRegionsFindsEndFromHighestPeakNotEarlierLocalPeak() {
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 0.0),
                                CasePoint(20.0, 1.0),
                                CasePoint(40.0, 11.0),
                                CasePoint(60.0, 21.0),
                                CasePoint(80.0, 19.0),
                                CasePoint(100.0, 29.0),
                                CasePoint(120.0, 39.0),
                                CasePoint(140.0, 30.0),
                                CasePoint(160.0, 29.5),
                                CasePoint(180.0, 29.0),
                                CasePoint(200.0, 29.0),
                        )
                )

        assertEquals(1, regions.size)
        assertEquals(6, regions[0].peakIndex)
        assertEquals(9, regions[0].endIndex)
        assertEquals(180.0, regions[0].endPoint.x, 0.0)
    }

    @Test
    fun findSlopeRegionsIgnoresRegionsWithXDistanceLessThan55() {
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 0.0),
                                CasePoint(10.0, 1.0),
                                CasePoint(20.0, 11.0),
                                CasePoint(30.0, 1.0),
                                CasePoint(40.0, 0.0),
                        )
                )

        assertTrue(regions.isEmpty())
    }

    @Test
    fun findSlopeRegionsFindsEndAfterFlatTopThenDownwardSlope() {
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 0.0),
                                CasePoint(20.0, 1.0),
                                CasePoint(40.0, 11.0),
                                CasePoint(60.0, 21.0),
                                CasePoint(80.0, 31.0),
                                CasePoint(100.0, 30.0),
                                CasePoint(120.0, 20.0),
                                CasePoint(140.0, 10.0),
                                CasePoint(160.0, 9.0),
                                CasePoint(180.0, 9.0),
                        )
                )

        assertEquals(1, regions.size)
        assertEquals(1, regions[0].startIndex)
        assertEquals(7, regions[0].endIndex)
    }

    @Test
    fun findSlopeRegionsSkipsFlatEndTooCloseToPeak() {
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 0.0),
                                CasePoint(20.0, 1.0),
                                CasePoint(40.0, 11.0),
                                CasePoint(60.0, 21.0),
                                CasePoint(80.0, 31.0),
                                CasePoint(90.0, 25.0),
                                CasePoint(100.0, 24.5),
                                CasePoint(120.0, 14.5),
                                CasePoint(140.0, 4.5),
                                CasePoint(160.0, 3.5),
                                CasePoint(180.0, 3.5),
                        )
                )

        assertEquals(1, regions.size)
        assertEquals(1, regions[0].startIndex)
        assertEquals(8, regions[0].endIndex)
    }

}
