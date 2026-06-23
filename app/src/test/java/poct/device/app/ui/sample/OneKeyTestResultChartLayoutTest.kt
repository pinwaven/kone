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

        val chartData = buildOneKeyChartData(points)

        assertEquals(1, chartData.slopeRegions.size)
        assertEquals(1800.0, chartData.slopeRegions[0].area, 0.0001)
        assertEquals(5, chartData.entrySets.size)
        assertTrue(chartData.entrySets[0].size == points.size)
    }
}
