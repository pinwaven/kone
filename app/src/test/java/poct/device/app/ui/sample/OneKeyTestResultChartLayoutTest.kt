package poct.device.app.ui.sample

import android.content.pm.ActivityInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

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
}
