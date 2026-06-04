package poct.device.app.thirdparty.nano

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import poct.device.app.R
import poct.device.app.thirdparty.model.nano.NanoAuthState
import poct.device.app.thirdparty.model.nano.NanoAuthSupport

class NanoAuthSupportTest {
    @Test
    fun activationErrorMapsToLocalizedStringResource() {
        assertEquals(
            R.string.nano_auth_error_firmware_id_mismatch,
            NanoAuthSupport.messageResForError("firmware_id_mismatch")
        )
        assertEquals(
            R.string.nano_auth_error_mainboard_id_mismatch,
            NanoAuthSupport.messageResForError("mainboard_id_mismatch")
        )
        assertEquals(
            R.string.nano_auth_error_no_available_machine_no,
            NanoAuthSupport.messageResForError("no_available_machine_no")
        )
    }

    @Test
    fun authStateKnowsWhetherActivationTokensExist() {
        assertFalse(NanoAuthState().isActivated())

        val state = NanoAuthState(
            rootToken = "root",
            commToken = "comm",
            commTokenExpiresAt = "2026-06-09T00:00:00.000Z",
            machineNo = "KNA1-F05103"
        )

        assertTrue(state.isActivated())
    }

    @Test
    fun tokenRedactionNeverReturnsFullToken() {
        assertEquals("(empty)", NanoAuthSupport.redactToken(""))
        assertEquals("***", NanoAuthSupport.redactToken("abc"))
        assertEquals("abcd...wxyz", NanoAuthSupport.redactToken("abcdefghijklmnopqrstuvwxyz"))
    }

    @Test
    fun firmwareIdCanBeExtractedFromHandshake() {
        assertEquals("V1.2.3", NanoAuthSupport.extractFirmwareId("ok ver:V1.2.3"))
        assertEquals("V1.2.3", NanoAuthSupport.extractFirmwareId("V1.2.3"))
        assertEquals("", NanoAuthSupport.extractFirmwareId("   "))
    }

    @Test
    fun firmwareIdUsesIdPartAfterVersionDelimiter() {
        assertEquals(
            "003000373931500620383958",
            NanoAuthSupport.extractFirmwareId("ok ver:v0.1.7~003000373931500620383958")
        )
    }
}
