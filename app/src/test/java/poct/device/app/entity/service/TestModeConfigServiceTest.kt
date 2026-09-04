package poct.device.app.entity.service

import org.junit.Assert.assertEquals
import org.junit.Test
import poct.device.app.bean.ConfigTestModeBean
import poct.device.app.bean.CaseBean
import poct.device.app.bean.card.CardConfig
import poct.device.app.bean.card.CardStatus

class TestModeConfigServiceTest {
    @Test
    fun normalizeReactionTimeKeepsPositiveSeconds() {
        assertEquals("60", TestModeConfigService.normalizeReactionTimeSeconds("60"))
    }

    @Test
    fun normalizeReactionTimeUsesDefaultForBlankOrInvalidValue() {
        assertEquals(
            ConfigTestModeBean.DEFAULT_REACTION_TIME_SECONDS,
            TestModeConfigService.normalizeReactionTimeSeconds("")
        )
        assertEquals(
            ConfigTestModeBean.DEFAULT_REACTION_TIME_SECONDS,
            TestModeConfigService.normalizeReactionTimeSeconds("abc")
        )
        assertEquals(
            ConfigTestModeBean.DEFAULT_REACTION_TIME_SECONDS,
            TestModeConfigService.normalizeReactionTimeSeconds("0")
        )
    }

    @Test
    fun applyReactionTimeOverridesCardConfigCutOff2() {
        val config = CardConfig(cutOff2 = 10.0)

        TestModeConfigService.applyReactionTimeSeconds(config, "45")

        assertEquals(45.0, config.cutOff2, 0.0)
    }

    @Test
    fun applyReactionTimeUsesDefaultWhenSecondsInvalid() {
        val config = CardConfig(cutOff2 = 10.0)

        TestModeConfigService.applyReactionTimeSeconds(config, "abc")

        assertEquals(300.0, config.cutOff2, 0.0)
    }

    @Test
    fun normalizeAbsorbTimeKeepsMillisecondsInsideRange() {
        assertEquals("0", TestModeConfigService.normalizeAbsorbTimeMillis("0"))
        assertEquals("300000", TestModeConfigService.normalizeAbsorbTimeMillis("300000"))
        assertEquals("240000", TestModeConfigService.normalizeAbsorbTimeMillis("240000"))
    }

    @Test
    fun normalizeAbsorbTimeClampsOutsideRangeAndDefaultsWhenInvalid() {
        assertEquals("0", TestModeConfigService.normalizeAbsorbTimeMillis("-1"))
        assertEquals("300000", TestModeConfigService.normalizeAbsorbTimeMillis("300001"))
        assertEquals("240000", TestModeConfigService.normalizeAbsorbTimeMillis("abc"))
    }

    @Test
    fun normalizeScanTimeKeepsMillisecondsInsideRange() {
        assertEquals("1", TestModeConfigService.normalizeScanTimeMillis("1"))
        assertEquals("300000", TestModeConfigService.normalizeScanTimeMillis("300000"))
        assertEquals("14000", TestModeConfigService.normalizeScanTimeMillis("14000"))
    }

    @Test
    fun normalizeScanTimeClampsOutsideRangeAndDefaultsWhenInvalid() {
        assertEquals("1", TestModeConfigService.normalizeScanTimeMillis("0"))
        assertEquals("300000", TestModeConfigService.normalizeScanTimeMillis("300001"))
        assertEquals("14000", TestModeConfigService.normalizeScanTimeMillis("abc"))
    }

    @Test
    fun normalizeLaserPowerKeepsValueInsideRange() {
        assertEquals("-100", TestModeConfigService.normalizeLaserPower("-100"))
        assertEquals("0", TestModeConfigService.normalizeLaserPower("0"))
        assertEquals("-25", TestModeConfigService.normalizeLaserPower("-25"))
    }

    @Test
    fun normalizeLaserPowerClampsOutsideRangeAndDefaultsWhenInvalid() {
        assertEquals("-100", TestModeConfigService.normalizeLaserPower("-101"))
        assertEquals("0", TestModeConfigService.normalizeLaserPower("1"))
        assertEquals("-25", TestModeConfigService.normalizeLaserPower("abc"))
    }

    @Test
    fun applyTestModeConfigOverridesCardConfigCutOffFields() {
        val config = CardConfig(xt1 = 10, cutOff1 = 10.0, cutOff2 = 20.0)

        TestModeConfigService.applyConfig(
            config = config,
            absorbTimeMillis = "3000",
            reactionTimeSeconds = "45",
            laserPower = "-30"
        )

        assertEquals(3, config.xt1)
        assertEquals(-30.0, config.cutOff1, 0.0)
        assertEquals(45.0, config.cutOff2, 0.0)
    }

    @Test
    fun buildLocalCardInfoUsesFixedCardCodeAndLocalTestConfig() {
        val cardInfo = TestModeConfigService.buildLocalCardInfo(
            cardCode = "KNC52286620-0100",
            config = ConfigTestModeBean(
                absorbTimeMillis = "3000",
                reactionTimeSeconds = "120",
                scanTimeMillis = "8000",
                laserPower = "-35"
            )
        )

        assertEquals("KNC52286620-0100", cardInfo.card.code)
        assertEquals(CardStatus.ACTIVE.statusVal, cardInfo.card.status)
        assertEquals("KNC52286620", cardInfo.cardBatch.code)
        assertEquals(CaseBean.TYPE_BIOAGE_CRP, cardInfo.cardBatch.type)
        assertEquals(3, cardInfo.cardConfig.xt1)
        assertEquals(-35.0, cardInfo.cardConfig.cutOff1, 0.0)
        assertEquals(120.0, cardInfo.cardConfig.cutOff2, 0.0)
    }
}
