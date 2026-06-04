package poct.device.app.thirdparty.nano

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.thirdparty.model.nano.NanoDeviceInfoSupport
import poct.device.app.thirdparty.model.nano.NanoMachine

class NanoDeviceInfoTest {
    @Test
    fun mapsRemoteMachineFieldsToSysFunInfoConfig() {
        val config = NanoDeviceInfoSupport.toConfigInfo(
            NanoMachine(
                machineNo = "KNA1-001",
                machineName = "Kino Lab 1",
                model = "KNA1",
                softwareVersion = "0.3.0",
                firmwareVersion = "v0.1.7",
                status = "active",
            )
        )

        assertEquals(
            ConfigInfoV2Bean(
                name = "Kino Lab 1",
                code = "KNA1-001",
                type = "KNA1",
                software = "0.3.0",
                hardware = "v0.1.7",
            ),
            config
        )
    }

    @Test
    fun rejectsRemoteMachineWithoutRequiredDisplayIdentity() {
        assertNull(NanoDeviceInfoSupport.toConfigInfo(NanoMachine(machineNo = "", model = "KNA1")))
        assertNull(NanoDeviceInfoSupport.toConfigInfo(NanoMachine(machineNo = "KNA1-001", model = "")))
    }
}
