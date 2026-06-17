package poct.device.app.ui.sysfun

import org.junit.Assert.assertEquals
import org.junit.Test

class SysFunApiTestTest {
    @Test
    fun initialTabDefaultsToProbe() {
        assertEquals(0, initialSysFunApiTestTab(null))
        assertEquals(0, initialSysFunApiTestTab(false))
    }

    @Test
    fun initialTabSwitchesToUpgradeWhenRequested() {
        assertEquals(1, initialSysFunApiTestTab(true))
    }
}
