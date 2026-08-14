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

    @Test
    fun findSlopeRegionsDetectsAllFourWavesIncludingSlowRiseSlowFallOnes() {
        // 真机采样：四个波，其中第 1 波（约 77~369）和第 3 波（约 938~1179）
        // 升降缓慢，近窗跌幅不足曾导致漏检。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_four_regions.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        assertEquals(4, regions.size)
        // 第 1 波（慢升慢降，期望约 77~369）
        assertEquals(81, regions[0].startIndex)
        assertEquals(376, regions[0].endIndex)
        assertEquals(238, regions[0].peakIndex)
        // 第 2 波（上升段有肩部平台、峰后双峰，期望约 380~669）
        assertEquals(386, regions[1].startIndex)
        assertEquals(665, regions[1].endIndex)
        assertEquals(511, regions[1].peakIndex)
        // 第 3 波（慢升，紧邻第 4 波，期望约 938~1179）
        assertEquals(924, regions[2].startIndex)
        assertEquals(1185, regions[2].endIndex)
        assertEquals(1036, regions[2].peakIndex)
        // 第 4 波
        assertEquals(1199, regions[3].startIndex)
        assertEquals(1484, regions[3].endIndex)
        assertEquals(1323, regions[3].peakIndex)
    }

    @Test
    fun findSlopeRegionsKeepsFullWidthOnBroadRoundedTopWave() {
        // 真机采样：第 1 波是宽、圆顶的缓慢波(≈81~306)。触发点在上升沿时左边界能走到波脚，但
        // "以最终波峰重新回推左边界"从圆顶波峰起算时，顶部近乎平坦（净上升≈0）当场停住，把已正确
        // 的起点向右收窄到波形内部，得到过窄的 ≈137~273。修复后回推只允许向左延伸，不再收缩。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_broad_rounded_top.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        // 第 1 波（期望约 81~306，主峰 idx194）覆盖完整波形
        assertEquals(78, regions[0].startIndex)
        assertEquals(317, regions[0].endIndex)
        assertEquals(194, regions[0].peakIndex)
    }

    @Test
    fun findSlopeRegionsFiltersSmallNoiseWaveByDefault() {
        // 主波(波幅100) + 一个通过绝对下限但相对主波极小的噪声小波(波幅8 < 10%)。
        // 默认开启噪声过滤 -> 小波被丢弃；关闭 -> 保留。
        val points =
                listOf(
                        CasePoint(0.0, 0.0),
                        CasePoint(20.0, 50.0),
                        CasePoint(40.0, 100.0),
                        CasePoint(60.0, 50.0),
                        CasePoint(80.0, 0.0),
                        CasePoint(100.0, 0.0),
                        CasePoint(120.0, 0.0),
                        CasePoint(140.0, 0.0),
                        CasePoint(160.0, 4.0),
                        CasePoint(180.0, 8.0),
                        CasePoint(200.0, 4.0),
                        CasePoint(220.0, 0.0),
                        CasePoint(240.0, 0.0),
                )

        val filtered = TestModePointChartData.findSlopeRegions(points, minPeakTroughYDiff = 5.0)
        assertEquals(1, filtered.size)
        assertEquals(40.0, filtered[0].peakPoint.x, 0.0)

        val unfiltered =
                TestModePointChartData.findSlopeRegions(
                        points,
                        minPeakTroughYDiff = 5.0,
                        filterNoiseWaves = false,
                )
        assertEquals(2, unfiltered.size)
    }

    @Test
    fun findSlopeRegionsKeepsComparableWavesWhenFiltering() {
        // 两个量级相近的波(波幅100与90)都应保留，不误删。
        val points =
                listOf(
                        CasePoint(0.0, 0.0),
                        CasePoint(20.0, 50.0),
                        CasePoint(40.0, 100.0),
                        CasePoint(60.0, 50.0),
                        CasePoint(80.0, 0.0),
                        CasePoint(100.0, 0.0),
                        CasePoint(120.0, 0.0),
                        CasePoint(140.0, 0.0),
                        CasePoint(160.0, 45.0),
                        CasePoint(180.0, 90.0),
                        CasePoint(200.0, 45.0),
                        CasePoint(220.0, 0.0),
                        CasePoint(240.0, 0.0),
                )

        val regions = TestModePointChartData.findSlopeRegions(points, minPeakTroughYDiff = 5.0)
        assertEquals(2, regions.size)
    }

    @Test
    fun findSlopeRegionsFollowsThroughShoulderToTroughOnFirstWave() {
        // 真机采样：第 1 波下降沿(峰 idx217)上有个短暂的肩部平台(≈idx316~324)，尾部平稳计数在此
        // 误触发停止，末端停在 ≈319/323，而其后仍有明显下降通向真正波谷(≈idx370)后升入第 2 波。
        // 修复：前方仍有大幅净下降(绝对量)时越过肩部继续，走到真正波谷 370。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_shoulder_before_trough.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        // 第 1 波（期望末端 ≈370，波峰 idx217）
        assertEquals(217, regions[0].peakIndex)
        assertEquals(370, regions[0].endIndex)
    }

    @Test
    fun findSlopeRegionsDoesNotEmitDuplicateForSameSecondPeak() {
        // 真机采样：第 2 波（削顶峰 idx1014）的下降尾部有个小凸起(≈idx1296)，它触发 isPeak 后左边界
        // 越过波峰回到波脚(888)，用同一个波峰重新检出一个区域，得到 (888,1145) 与 (888,1355) 两个
        // 重叠波。修复：新区域若与上一个已接收区域重叠即判为重复，丢弃。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_duplicate_second_peak.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        // 第 2 波只应出现一次
        val atSecondPeak = regions.filter { it.startIndex <= 1014 && 1014 <= it.endIndex }
        assertEquals(1, atSecondPeak.size)
        assertEquals(888, atSecondPeak.first().startIndex)
        assertEquals(1145, atSecondPeak.first().endIndex)
        assertEquals(1014, atSecondPeak.first().peakIndex)
    }

    @Test
    fun findSlopeRegionsCrossesDoubleBumpTopToReachLeftFoot() {
        // 真机采样：第 2 波的波顶是双峰——上升沿先到一个小凸起(≈454)，一个浅凹陷(≈466)后再升到主峰(516)。
        // 左边界回退在峰间浅凹处被小凸起挡住（net-rise 看相邻更高、误判到脚），起点卡在波形内部(≈476)。
        // 修复：窗口内更左仍有明显更低地面时越过凸起，起点回到真正波脚(≈385)。
        // （末端为缓降尾，按既定策略停在拐点附近 ≈668，不追到基线波谷，避免回归其他已确认波形。）
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_double_bump_left.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        // 第 2 波（双峰顶，期望起点 ≈385）起点回到真正波脚
        val second = regions.firstOrNull { it.startIndex <= 516 && 516 <= it.endIndex }
        assertEquals(387, second?.startIndex)
        assertEquals(516, second?.peakIndex)
    }

    @Test
    fun findSlopeRegionsDetectsWideFlatTopClippedWaveBeyondPeakWindow() {
        // 真机采样：削顶饱和波，平顶平台宽 132（956~1087），超过 2×WIDE_WINDOW_X(100)。平台内部
        // 没有一点能同时在 50x 内看到两侧跌落，isPeak 全程为假，第 3 波(≈895~1205)完全漏检。
        // 修复：加宽平顶兜底触发（isWideFlatTopOnset），仅在 isPeak 覆盖不到的宽平顶前沿补触发。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_wide_flat_top.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        // 第 3 波（宽平顶，期望约 895~1205）必须被检出
        val third = regions.firstOrNull { it.startIndex <= 1050 && 1050 <= it.endIndex }
        assertEquals(901, third?.startIndex)
        assertEquals(1198, third?.endIndex)
        assertEquals(956, third?.peakIndex)
    }

    @Test
    fun findSlopeRegionsKeepsFullDescentOnWideSlowWave() {
        // 真机采样：第 2 波是缓慢的宽波，起点在波谷、缓升到峰(idx971)再缓降到基线，整段跨度约 480，
        // 超过原跨度上限 400，被从起点截断（尾部下降段丢失），末端停在 ≈1059 而非真正波谷 ≈1134。
        // 修复：把跨度上限放宽到 500，覆盖最宽单波。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_wide_slow_span.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        // 第 2 波（缓宽波，期望末端 ≈1134）末端覆盖完整下降段
        val second = regions.firstOrNull { it.startIndex <= 971 && 971 <= it.endIndex }
        assertEquals(659, second?.startIndex)
        assertEquals(1138, second?.endIndex)
        assertEquals(971, second?.peakIndex)
    }

    @Test
    fun findSlopeRegionsDetectsClippedWaveWithNoisyPlateauTop() {
        // 真机采样：削顶饱和波，但平台带纹波（≈8157~8189，并非严格等值）。触发点落在噪声平台内部，
        // 左边界"严格低于波峰"的护栏被平台纹波击穿（相邻点低于触发值即当场停住），起点锚死在平台上、
        // 波幅≈0 被丢弃，第 2 波(≈351~602)漏检。修复：改用相对波峰的顶部容差带（minPeakTroughYDiff），
        // 足以容纳削顶纹波，先越过平台再走到波脚。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_noisy_clipped_top.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        // 第 2 波（削顶噪声平台，期望约 351~602）必须被检出
        val second = regions.firstOrNull { it.startIndex <= 470 && 470 <= it.endIndex }
        assertEquals(352, second?.startIndex)
        assertEquals(606, second?.endIndex)
        assertEquals(438, second?.peakIndex)
    }

    @Test
    fun findSlopeRegionsDetectsFlatToppedClippedWaveWithWidePlateau() {
        // 真机采样：ADC 削顶饱和，波峰是一段等值平台(8190)。isPeak 在宽平台内部触发，但左边界
        // 的"净上升"窗口整段落在平台上（净差 0），当场停住 → 起点=波峰、波幅=0，第 4 波(≈860~1140)
        // 被丢弃（窄平台的波因窗口能够到上升沿而未受影响）。修复后仅在降到波峰高度之下才允许平坦
        // 停止，先越过平台走到上升沿再到波脚。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_flat_top_clipped.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        // 第 4 波（削顶平台 959~1045，期望约 860~1140）必须被检出
        val fourth = regions.firstOrNull { it.startIndex <= 1000 && 1000 <= it.endIndex }
        assertEquals(840, fourth?.startIndex)
        assertEquals(1136, fourth?.endIndex)
        assertEquals(959, fourth?.peakIndex)
    }

    @Test
    fun findSlopeRegionsDoesNotSkipFirstWaveWhenBaselineBumpTriggersFirst() {
        // 真机采样：第 1 波之前基线上有个幅度约 30 的宽凸起(≈idx200)先触发 isPeak，其向后搜索
        // 越过第 1 波、findHighestPoint 把波峰迁移到更高的第 2 波上，区域被判成第 2 波，游标随即
        // 跳过第 1 波（漏检，只剩 ≈889~1214）。此外该波下降段汇入一段带纹波的平坦基线，尾部曾被
        // 单点纹波尖跌误判为"仍在下降"而拖到很靠后。修复后第 1 波应被检出且尾部收在拐点附近。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_first_baseline_bump_skip.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        // 第 1 波（期望约 373~636，主峰 idx488）必须被检出，且不与基线凸起或第 2 波混淆
        assertEquals(367, regions[0].startIndex)
        assertEquals(660, regions[0].endIndex)
        assertEquals(488, regions[0].peakIndex)
    }

    @Test
    fun findSlopeRegionsKeepsDoubleHumpWaveWholeInsteadOfCuttingAtShoulderDip() {
        // 真机采样：第 1 波是双峰——主峰(≈idx212)后有一个浅肩凹陷(≈idx243)再回升到次峰，
        // 之后才真正下降到基线(≈idx357)。"谷底回升即视为下一个波"用的是绝对阈值 50，对幅度
        // 数千的 ADC 波形过于敏感，肩部浅凹再回升就被当成新波，把区域截在 ≈102~243（下降段全丢）。
        // 修复后按相对波幅判定谷底深度，浅肩凹陷不再被误判，区域覆盖完整波形。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_double_hump_first.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        // 第 1 波（期望约 88~357，主峰 idx212），不在肩部凹陷处被截断
        assertEquals(81, regions[0].startIndex)
        assertEquals(357, regions[0].endIndex)
        assertEquals(212, regions[0].peakIndex)
    }

    @Test
    fun findSlopeRegionsFindsFullWidthWavesOverLongRipplingBaseline() {
        // 真机采样：两个大波之间隔着很长的带纹波基线。逐点坡度判据把基线纹波误当作"上升仍在
        // 继续"，左边界一路爬到极左（第 1 波爬到 ~68、第 2 波爬到 ~616），随后 MAX_REGION_X
        // 从错误起点截断，把整个下降段砍掉，两波都只剩上升沿+峰顶（曾检出 ~365~468 与 864~1016，
        // 且第 1 波还重复检出一次）。修复后应覆盖完整波形。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_two_slow_baseline.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        assertEquals(2, regions.size)
        // 第 1 波（期望约 337~631）
        assertEquals(333, regions[0].startIndex)
        assertEquals(631, regions[0].endIndex)
        assertEquals(470, regions[0].peakIndex)
        // 第 2 波（期望约 853~1144）
        assertEquals(849, regions[1].startIndex)
        assertEquals(1131, regions[1].endIndex)
        assertEquals(970, regions[1].peakIndex)
    }

    @Test
    fun findSlopeRegionsDoesNotEmitSpuriousRegionInsideFirstWave() {
        // 真机采样：起始段有幅度约 30 的噪声小包（74~130），曾把区域起点锚死在噪声上，
        // 再经跨度上限截断产生 393~477 的假波，叠在真实第 1 波（约 352~635）内部。
        val values =
                requireNotNull(javaClass.classLoader?.getResourceAsStream("wave_noise_bump.txt"))
                        .bufferedReader()
                        .readText()
                        .split(',')
                        .map { it.trim().toDouble() }
        val points = values.mapIndexed { index, y -> CasePoint(index.toDouble(), y) }

        val regions = TestModePointChartData.findSlopeRegions(points)

        assertEquals(3, regions.size)
        // 第 1 波（期望约 352~635），无内部假波
        assertEquals(359, regions[0].startIndex)
        assertEquals(634, regions[0].endIndex)
        assertEquals(487, regions[0].peakIndex)
        // 第 2 波
        assertEquals(876, regions[1].startIndex)
        assertEquals(1175, regions[1].endIndex)
        assertEquals(1028, regions[1].peakIndex)
        // 第 3 波
        assertEquals(1182, regions[2].startIndex)
        assertEquals(1386, regions[2].endIndex)
        assertEquals(1282, regions[2].peakIndex)
    }

}
