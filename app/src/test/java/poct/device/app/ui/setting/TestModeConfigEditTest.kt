package poct.device.app.ui.setting

import org.junit.Assert.assertEquals
import org.junit.Test

class TestModeConfigEditTest {
    @Test
    fun cancelRestoresEditedReactionTimeToOriginalValue() {
        val edited = TestModeConfigValues(
            reactionTimeSeconds = "45",
            absorbTimeMillis = "3000",
            scanTimeMillis = "14000",
            laserPower = "-25",
        )

        val restored = restoreTestModeConfigValue(
            values = edited,
            field = TestModeConfigField.REACTION_TIME,
            originalValue = "300",
        )

        assertEquals("300", restored.reactionTimeSeconds)
        assertEquals("3000", restored.absorbTimeMillis)
        assertEquals("14000", restored.scanTimeMillis)
        assertEquals("-25", restored.laserPower)
    }

    @Test
    fun cancelRestoresOnlyTheEditedField() {
        val edited = TestModeConfigValues(
            reactionTimeSeconds = "300",
            absorbTimeMillis = "9999",
            scanTimeMillis = "8888",
            laserPower = "-50",
        )

        val restored = restoreTestModeConfigValue(
            values = edited,
            field = TestModeConfigField.LASER_POWER,
            originalValue = "-25",
        )

        assertEquals("300", restored.reactionTimeSeconds)
        assertEquals("9999", restored.absorbTimeMillis)
        assertEquals("8888", restored.scanTimeMillis)
        assertEquals("-25", restored.laserPower)
    }
}
