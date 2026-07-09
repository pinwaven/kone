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
    fun toEntrySetsAddsRegionLinesPeakPointsStartPointsAndEndPoints() {
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
        val region =
                TestModePointChartData.findSlopeRegions(points, minPeakTroughYDiff = 5.0).first()

        val entrySets = TestModePointChartData.toEntrySets(points, listOf(region))

        // Series: main curve, region line, peak point, start point, end point
        assertEquals(5, entrySets.size)
        assertEquals(points.size, entrySets[0].size)
        assertEquals(2, entrySets[1].size)
        assertEquals(1, entrySets[2].size)
        assertEquals(1, entrySets[3].size)
        assertEquals(1, entrySets[4].size)
        // Peak point
        assertEquals(80f, entrySets[2][0].x, 0.0f)
        assertEquals(31f, entrySets[2][0].y, 0.0f)
        // Start point
        assertEquals(20f, entrySets[3][0].x, 0.0f)
        assertEquals(1f, entrySets[3][0].y, 0.0f)
        // End point
        assertEquals(140f, entrySets[4][0].x, 0.0f)
        assertEquals(1f, entrySets[4][0].y, 0.0f)
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
                        ),
                        minPeakTroughYDiff = 5.0,
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
                        ),
                        minPeakTroughYDiff = 5.0,
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
                        ),
                        minPeakTroughYDiff = 5.0,
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
                        ),
                        minPeakTroughYDiff = 5.0,
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
                        ),
                        minPeakTroughYDiff = 5.0,
                )

        assertEquals(1, regions.size)
        assertEquals(1, regions[0].startIndex)
        // findMinInFlatTail: x=140(y=10) < x=160(y=9), minimum is at index 8 (x=160)
        assertEquals(8, regions[0].endIndex)
    }

    @Test
    fun findSlopeRegionsFindsMultipleRegionsEvenWithNoiseDipAtTrough() {
        // Region 1 ends at a trough. A single noise dip (one steep segment, slope -0.27)
        // sits 40 x-units after the trough before Region 2 begins rising.
        // The algorithm must NOT let this single noise segment extend Region 1 past the trough
        // and consume Region 2.
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 5.0),
                                CasePoint(20.0, 6.0),
                                CasePoint(40.0, 16.0),
                                CasePoint(60.0, 26.0),
                                CasePoint(80.0, 36.0),  // Peak1
                                CasePoint(100.0, 26.0),
                                CasePoint(120.0, 16.0),
                                CasePoint(140.0, 6.0),
                                CasePoint(160.0, 5.5),  // trough of Region 1
                                CasePoint(180.0, 5.0),
                                CasePoint(200.0, 4.7),
                                CasePoint(210.0, 2.0),  // noise dip (slope ≈ -0.27, one segment)
                                CasePoint(220.0, 4.5),  // recovers immediately
                                CasePoint(240.0, 5.0),
                                CasePoint(260.0, 15.0),
                                CasePoint(280.0, 25.0), // Peak2
                                CasePoint(300.0, 15.0),
                                CasePoint(320.0, 5.0),
                                CasePoint(340.0, 4.5),
                                CasePoint(360.0, 4.5),
                        ),
                        minPeakTroughYDiff = 5.0,
                )

        assertEquals("Both regions must be detected", 2, regions.size)
        assertEquals("First region peak at x=80", 80.0, regions[0].peakPoint.x, 0.0)
        assertEquals("Second region peak at x=280", 280.0, regions[1].peakPoint.x, 0.0)
    }

    @Test
    fun findSlopeRegionsFindsEndAfterSteepDropFollowingGentleDownslope() {
        // Pattern: rise → peak → gentle downslope → brief flat → steep drop → flat bottom
        // Bug: old algorithm stops at the brief flat between gentle and steep slopes.
        // Fix: look ahead and continue if a steep drop follows within lookahead distance.
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 0.0),
                                CasePoint(20.0, 1.0),
                                CasePoint(40.0, 11.0),
                                CasePoint(60.0, 21.0),
                                CasePoint(80.0, 31.0),  // peak (index 4)
                                CasePoint(100.0, 26.8), // gentle down (slope ≈ -0.21)
                                CasePoint(120.0, 22.6), // gentle down (slope ≈ -0.21)
                                CasePoint(140.0, 18.4), // gentle down (slope ≈ -0.21)
                                CasePoint(160.0, 17.9), // brief flat (slope = -0.025) — old end
                                CasePoint(180.0, 7.9),  // steep drop (slope = -0.5)
                                CasePoint(200.0, 0.4),  // steep drop (slope ≈ -0.375)
                                CasePoint(220.0, 0.1),  // near-flat (slope = -0.015) — true end
                                CasePoint(240.0, 0.1),
                        ),
                        minPeakTroughYDiff = 5.0,
                )

        assertEquals(1, regions.size)
        assertTrue(
                "End should be after the steep drop, not at the brief flat mid-descent",
                regions[0].endPoint.x >= 200.0,
        )
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
                        ),
                        minPeakTroughYDiff = 5.0,
                )

        assertEquals(1, regions.size)
        assertEquals(1, regions[0].startIndex)
        // findMinInFlatTail: x=140(y=4.5) < x=160(y=3.5), minimum is at index 9 (x=160)
        assertEquals(9, regions[0].endIndex)
    }

    @Test
    fun findSlopeRegionsBacksUpToRiseStartWhenClampedBoundaryIsMidRise() {
        // startIndex x=0, maxEndX=320.
        // After peak (x=80), flat tail descends to local min at x=200 (y=5),
        // then gently rises through boundary (slope≈0.0125 per unit, <=0.2 so findMinInFlatTail
        // does not break there), then descends beyond boundary to deeper min at x=440 (y=2).
        // findMinInFlatTail returns index 12 (x=440 > 320) → clamping triggers.
        // Clamped boundary at x=320 is mid-rise → endIndex should back up to x=200 (local min).
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 0.0),
                                CasePoint(20.0, 5.0),
                                CasePoint(60.0, 20.0),
                                CasePoint(80.0, 30.0),  // peak (index 3)
                                CasePoint(120.0, 15.0),
                                CasePoint(160.0, 8.0),
                                CasePoint(200.0, 5.0),  // local min — expected endIndex=6
                                CasePoint(240.0, 5.5),  // gentle rise (slope 0.0125)
                                CasePoint(280.0, 6.0),
                                CasePoint(320.0, 6.5),  // boundary x=320, still rising
                                CasePoint(360.0, 5.0),
                                CasePoint(400.0, 3.0),
                                CasePoint(440.0, 2.0),  // deeper min beyond boundary
                                CasePoint(480.0, 2.5),
                                CasePoint(500.0, 8.0),  // upswing — findMinInFlatTail breaks here
                        ),
                        minPeakTroughYDiff = 5.0,
                )

        assertEquals(1, regions.size)
        assertEquals(6, regions[0].endIndex)
        assertEquals(200.0, regions[0].endPoint.x, 0.0)
    }


    @Test
    fun findSlopeRegionsShrinkEndWhenCurveDipsBelowStartEndLine() {
        // Start (0,10), peak (80,30), descent with uptick dip:
        // x=120 y=5 (dip), x=140 y=6 (uptick), x=160 y=4 (deeper minimum = initial end)
        // Line from (0,10) to (160,4): at x=120, line y=5.5 > curve y=5 → curve below line.
        // Fix: shrink end to x=120 where curve stays above line from (0,10) to (120,5).
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 10.0),
                                CasePoint(40.0, 20.0),
                                CasePoint(80.0, 30.0),
                                CasePoint(120.0, 5.0),
                                CasePoint(140.0, 6.0),
                                CasePoint(160.0, 4.0),
                                CasePoint(200.0, 4.5),
                        ),
                        minPeakTroughYDiff = 5.0,
                )

        assertEquals(1, regions.size)
        assertEquals(3, regions[0].endIndex)
        assertEquals(120.0, regions[0].endPoint.x, 0.0)
    }

    @Test
    fun findSlopeRegionsFiltersWavesWithPeakTroughDiffBelowDefaultThreshold() {
        // Same shape as findSlopeRegionsUsesPeakAndFlatSlopeBoundaries (amplitude 30),
        // but with the default threshold of 50 the wave must be filtered out.
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

        assertTrue(regions.isEmpty())
    }

    @Test
    fun findSlopeRegionsExtendsStartPastSmallLeftWaveAndRecalculatesEnd() {
        // A small wave (amplitude 30 < 50) sits left of the main wave and is filtered out.
        // The main wave's start must extend left past it to x=0. The MAX_REGION_X_DISTANCE
        // clamp is measured from the rise start (x=200), not the extended start, so the
        // end stays at the true trough (x=340) instead of being clamped back to x=320.
        val regions =
                TestModePointChartData.findSlopeRegions(
                        listOf(
                                CasePoint(0.0, 2.0),
                                CasePoint(20.0, 12.0),
                                CasePoint(40.0, 32.0),   // small wave peak
                                CasePoint(60.0, 12.0),
                                CasePoint(80.0, 4.0),
                                CasePoint(120.0, 3.5),
                                CasePoint(160.0, 3.0),
                                CasePoint(200.0, 3.0),   // trough where the left walk stops
                                CasePoint(220.0, 53.0),
                                CasePoint(240.0, 103.0),
                                CasePoint(260.0, 153.0), // main peak
                                CasePoint(280.0, 103.0),
                                CasePoint(300.0, 53.0),
                                CasePoint(320.0, 3.0),
                                CasePoint(340.0, 2.0),
                                CasePoint(360.0, 2.0),
                        )
                )

        assertEquals(1, regions.size)
        assertEquals(0, regions[0].startIndex)
        assertEquals(0.0, regions[0].startPoint.x, 0.0)
        assertEquals(260.0, regions[0].peakPoint.x, 0.0)
        assertEquals(14, regions[0].endIndex)
        assertEquals(340.0, regions[0].endPoint.x, 0.0)
    }

}
