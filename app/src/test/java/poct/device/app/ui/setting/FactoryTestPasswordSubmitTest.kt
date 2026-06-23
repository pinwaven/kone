package poct.device.app.ui.setting

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class FactoryTestPasswordSubmitTest {
    @Test
    fun correctPasswordClosesDialogClearsPasswordAndNavigates() {
        val result = submitFactoryTestPassword("5988") { it == "5988" }

        assertFalse(result.dialogVisible)
        assertEquals("", result.password)
        assertTrue(result.navigateToFactoryTest)
        assertFalse(result.showWrongPassword)
    }

    @Test
    fun wrongPasswordKeepsDialogOpenClearsPasswordAndShowsError() {
        val result = submitFactoryTestPassword("1234") { it == "5988" }

        assertTrue(result.dialogVisible)
        assertEquals("", result.password)
        assertFalse(result.navigateToFactoryTest)
        assertTrue(result.showWrongPassword)
    }
}
