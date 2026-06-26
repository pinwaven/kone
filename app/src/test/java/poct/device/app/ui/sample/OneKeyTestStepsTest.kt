package poct.device.app.ui.sample

import org.junit.Assert.assertEquals
import org.junit.Test

class OneKeyTestStepsTest {

    @Test
    fun `defaultOneKeyTestSteps has qr scan step at index 2 between chip insert and move in`() {
        val steps = defaultOneKeyTestSteps()

        assertEquals(7, steps.size)
        assertEquals("插入芯片", steps[1].label)
        assertEquals("扫描二维码", steps[2].label)
        assertEquals("片仓移入", steps[3].label)
    }

    @Test
    fun `pollQrCodeResult polls until first non-empty value`() {
        val responses = ArrayDeque(listOf("", "", "QR-TEST-123"))
        val result = pollQrCodeResult { responses.removeFirst() }
        assertEquals("QR-TEST-123", result)
    }
}
