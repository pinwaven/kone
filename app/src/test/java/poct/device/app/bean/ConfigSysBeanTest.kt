package poct.device.app.bean

import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigSysBeanTest {
    @Test
    fun flowDefaultsToNano() {
        assertEquals(ConfigSysBean.FLOW_NANO, ConfigSysBean().flow)
        assertEquals(ConfigSysBean.FLOW_NANO, ConfigSysBean.defaultFlow(""))
    }

    @Test
    fun defaultFlowKeepsExplicitClinicalSelection() {
        assertEquals(ConfigSysBean.FLOW_CLINICAL, ConfigSysBean.defaultFlow("clinical"))
    }
}
