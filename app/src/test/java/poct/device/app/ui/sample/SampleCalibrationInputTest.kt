package poct.device.app.ui.sample

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleCalibrationInputTest {
    @Test
    fun formatHsCrpForJsonKeepsNumbersUnquotedAndTrimsIntegerDecimal() {
        assertEquals("0", formatHsCrpForJson(0.0))
        assertEquals("5", formatHsCrpForJson(5.0))
        assertEquals("2.5", formatHsCrpForJson(2.5))
    }

    @Test
    fun filterDecimalInputKeepsDigitsAndOnlyFirstDot() {
        assertEquals("123", "a1b2c3".filterDecimalInput())
        assertEquals("1.23", "1.2.3".filterDecimalInput())
        assertEquals(".5", ".5mg/L".filterDecimalInput())
    }

    @Test
    fun defaultCalibrationStepsSwitchesFirstStepAfterInitialReset() {
        assertEquals("片仓复位", defaultCalibrationSteps(firstStepIsReset = true)[0].label)
        assertEquals("片仓弹出", defaultCalibrationSteps(firstStepIsReset = false)[0].label)
    }
}
