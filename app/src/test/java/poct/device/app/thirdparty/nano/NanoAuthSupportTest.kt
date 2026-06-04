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
    fun sensitiveJsonValuesAreRedactedBeforeLogging() {
        val text = """{"root_token":"root-secret-123456","comm_token":"comm-secret-654321","machine_no":"KNA1-001"}"""

        val redacted = NanoAuthSupport.redactSensitiveText(text)

        assertFalse(redacted.contains("root-secret-123456"))
        assertFalse(redacted.contains("comm-secret-654321"))
        assertTrue(redacted.contains("machine_no"))
        assertTrue(redacted.contains("***"))
    }

    @Test
    fun authStateCanBeInvalidatedWithoutLosingMachineIdentity() {
        val state = NanoAuthState(
            rootToken = "root-secret",
            commToken = "comm-secret",
            commTokenExpiresAt = "2026-06-09T00:00:00.000Z",
            machineNo = "KNA1-F05103",
            machineName = "Device 1",
            model = "KNA1",
            status = "active",
            activatedAt = "2026-06-04T00:00:00.000Z",
        )

        val invalidated = state.invalidated(
            reason = "invalid_root_token",
            timestamp = "2026-06-04T10:00:00.000Z",
        )

        assertFalse(invalidated.isActivated())
        assertEquals("", invalidated.rootToken)
        assertEquals("", invalidated.commToken)
        assertEquals("", invalidated.commTokenExpiresAt)
        assertEquals("KNA1-F05103", invalidated.machineNo)
        assertEquals("Device 1", invalidated.machineName)
        assertEquals("KNA1", invalidated.model)
        assertEquals("invalid_root_token", invalidated.status)
        assertEquals("2026-06-04T10:00:00.000Z", invalidated.refreshedAt)
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
