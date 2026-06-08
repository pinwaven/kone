package poct.device.app.utils.app

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceIdUtilsTest {
    @Test
    fun selectsSerialNumberFromFcReaderProviderFirst() {
        assertEquals(
            "FC123456",
            DeviceIdUtils.selectSn(
                fcReaderSn = " FC123456 ",
                systemPropertySns = listOf("SN123456"),
                buildSerial = "BUILD987"
            )
        )
    }

    @Test
    fun ignoresUnknownFcReaderProviderValue() {
        assertEquals(
            "SN123456",
            DeviceIdUtils.selectSn(
                fcReaderSn = "unknown",
                systemPropertySns = listOf("SN123456"),
                buildSerial = "BUILD987"
            )
        )
    }

    @Test
    fun selectsSerialNumberFromSystemProperty() {
        assertEquals(
            "SN123456",
            DeviceIdUtils.selectSn(systemPropertySns = listOf(" SN123456 "), buildSerial = "BUILD987")
        )
    }

    @Test
    fun fallsBackToBootSerialNumberWhenSerialNumberPropertyIsMissing() {
        assertEquals(
            "BOOT987",
            DeviceIdUtils.selectSn(systemPropertySns = listOf("", " BOOT987 "), buildSerial = "BUILD987")
        )
    }

    @Test
    fun fallsBackToBuildSerialWhenSystemPropertiesAreMissing() {
        assertEquals(
            "BUILD987",
            DeviceIdUtils.selectSn(systemPropertySns = listOf("", ""), buildSerial = " BUILD987 ")
        )
    }

    @Test
    fun ignoresUnknownBuildSerial() {
        assertEquals(
            "",
            DeviceIdUtils.selectSn(systemPropertySns = listOf("", ""), buildSerial = "unknown")
        )
    }

    @Test
    fun masksSerialNumberForLogs() {
        assertEquals("<empty>", DeviceIdUtils.maskForLog(""))
        assertEquals("****", DeviceIdUtils.maskForLog("1234"))
        assertEquals("SN***6789", DeviceIdUtils.maskForLog("SN123456789"))
    }

    @Test
    fun buildsShellGetpropCommands() {
        assertEquals(
            "getprop ro.boot.serialno",
            DeviceIdUtils.buildGetpropCommand("ro.boot.serialno")
        )
        assertEquals(
            "su -c 'getprop ro.boot.serialno'",
            DeviceIdUtils.buildSuGetpropCommand("ro.boot.serialno")
        )
    }
}
