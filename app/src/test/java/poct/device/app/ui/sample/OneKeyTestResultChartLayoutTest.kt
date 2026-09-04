package poct.device.app.ui.sample

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import poct.device.app.entity.CasePoint

class OneKeyTestResultChartLayoutTest {
    @Test
    fun chartDialogLocksLandscapeAndRestoresOrientationOnClose() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, oneKeyChartDialogOpenOrientation())
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            oneKeyChartDialogCloseOrientation(),
        )
        assertFalse(oneKeyChartDialogUsePlatformDefaultWidth())
    }

    @Test
    fun buildOneKeyChartDataCalculatesSlopeRegionsAndAreasLikeReportChart() {
        // buildOneKeyChartData calls findSlopeRegions with all defaults, including
        // DEFAULT_MIN_PEAK_TROUGH_Y_DIFF=50 — a peak amplitude of 31 (as this fixture
        // used to have) is below that floor and always gets rejected as "too narrow"
        // regardless of peak detection. Use amplitude 60 to actually pass detection.
        val points =
            listOf(
                CasePoint(0.0, 0.0),
                CasePoint(20.0, 15.0),
                CasePoint(40.0, 30.0),
                CasePoint(60.0, 45.0),
                CasePoint(80.0, 60.0),
                CasePoint(100.0, 45.0),
                CasePoint(120.0, 30.0),
                CasePoint(140.0, 15.0),
                CasePoint(160.0, 0.0),
            )

        val chartData = buildOneKeyChartData(points)

        assertEquals(1, chartData.slopeRegions.size)
        assertEquals(3600.0, chartData.slopeRegions[0].area, 0.0001)
        assertEquals(5, chartData.entrySets.size)
        assertTrue(chartData.entrySets[0].size == points.size)
    }
}
