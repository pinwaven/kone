package poct.device.app.thirdparty.nano

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import poct.device.app.thirdparty.model.nano.NanoMachineInfoReq
import poct.device.app.thirdparty.model.nano.NanoMachineInfoSupport

class NanoMachineInfoTest {
    private val gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create()

    @Test
    fun buildRequestTrimsVersionsAndSerializesSnakeCaseFields() {
        val request = NanoMachineInfoSupport.buildRequest(
            softwareVersion = " 0.3.0 ",
            firmwareVersion = " v0.1.7 ",
        )

        assertEquals(NanoMachineInfoReq("0.3.0", "v0.1.7"), request)
        assertEquals(
            """{"software_version":"0.3.0","firmware_version":"v0.1.7"}""",
            gson.toJson(request)
        )
    }

    @Test
    fun buildRequestOmitsBlankVersionsAndSkipsWhenBothBlank() {
        assertEquals(
            NanoMachineInfoReq(softwareVersion = "0.3.0", firmwareVersion = null),
            NanoMachineInfoSupport.buildRequest("0.3.0", "   ")
        )
        assertNull(NanoMachineInfoSupport.buildRequest(" ", "\n"))
    }

    @Test
    fun buildRequestRejectsOverlongValues() {
        val overlong = "x".repeat(129)

        assertNull(NanoMachineInfoSupport.buildRequest(overlong, "v0.1.7"))
        assertNull(NanoMachineInfoSupport.buildRequest("0.3.0", overlong))
    }
}
