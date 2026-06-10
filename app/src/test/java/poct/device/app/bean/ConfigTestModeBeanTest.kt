package poct.device.app.bean

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigTestModeBeanTest {
    @Test
    fun reactionTimeDefaultsToThreeHundredSeconds() {
        val bean = ConfigTestModeBean()

        assertEquals("300", bean.reactionTimeSeconds)
    }

    @Test
    fun testModeParametersUseExpectedDefaults() {
        val bean = ConfigTestModeBean()

        assertEquals("3000", bean.absorbTimeMillis)
        assertEquals("8000", bean.scanTimeMillis)
        assertEquals("-25", bean.laserPower)
    }

    @Test
    fun usesDedicatedSysConfigPrefix() {
        assertEquals("test_mode_", ConfigTestModeBean.PREFIX)
    }
}
