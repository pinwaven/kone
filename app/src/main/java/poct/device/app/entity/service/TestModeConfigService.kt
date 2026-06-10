package poct.device.app.entity.service

import poct.device.app.bean.ConfigTestModeBean
import poct.device.app.bean.CaseBean
import poct.device.app.bean.card.Card
import poct.device.app.bean.card.CardConfig
import poct.device.app.bean.card.CardBatch
import poct.device.app.bean.card.CardInfoBean
import poct.device.app.bean.card.CardStatus
import poct.device.app.utils.app.AppTypeUtils

object TestModeConfigService {
    suspend fun findBean(): ConfigTestModeBean {
        val bean = SysConfigService.findBean(
            ConfigTestModeBean.PREFIX,
            ConfigTestModeBean::class
        )
        bean.reactionTimeSeconds = normalizeReactionTimeSeconds(bean.reactionTimeSeconds)
        bean.absorbTimeMillis = normalizeAbsorbTimeMillis(bean.absorbTimeMillis)
        bean.scanTimeMillis = normalizeScanTimeMillis(bean.scanTimeMillis)
        bean.laserPower = normalizeLaserPower(bean.laserPower)
        return bean
    }

    suspend fun saveReactionTimeSeconds(seconds: String) {
        val bean = findBean()
        bean.reactionTimeSeconds = normalizeReactionTimeSeconds(seconds)
        saveBean(bean)
    }

    suspend fun saveAbsorbTimeMillis(milliseconds: String) {
        val bean = findBean()
        bean.absorbTimeMillis = normalizeAbsorbTimeMillis(milliseconds)
        saveBean(bean)
    }

    suspend fun saveScanTimeMillis(milliseconds: String) {
        val bean = findBean()
        bean.scanTimeMillis = normalizeScanTimeMillis(milliseconds)
        saveBean(bean)
    }

    suspend fun saveLaserPower(laserPower: String) {
        val bean = findBean()
        bean.laserPower = normalizeLaserPower(laserPower)
        saveBean(bean)
    }

    private suspend fun saveBean(bean: ConfigTestModeBean) {
        SysConfigService.saveBean(
            ConfigTestModeBean.PREFIX,
            bean
        )
    }

    fun normalizeReactionTimeSeconds(seconds: String): String {
        val parsed = seconds.trim().toIntOrNull()
        return if (parsed != null && parsed > 0) {
            parsed.toString()
        } else {
            ConfigTestModeBean.DEFAULT_REACTION_TIME_SECONDS
        }
    }

    fun applyReactionTimeSeconds(config: CardConfig, seconds: String): CardConfig {
        config.cutOff2 = normalizeReactionTimeSeconds(seconds).toDouble()
        return config
    }

    fun normalizeAbsorbTimeMillis(milliseconds: String): String {
        return normalizeIntInRange(
            value = milliseconds,
            min = 1500,
            max = 100000,
            defaultValue = ConfigTestModeBean.DEFAULT_ABSORB_TIME_MILLIS
        )
    }

    fun normalizeScanTimeMillis(milliseconds: String): String {
        return normalizeIntInRange(
            value = milliseconds,
            min = 1,
            max = 60000,
            defaultValue = ConfigTestModeBean.DEFAULT_SCAN_TIME_MILLIS
        )
    }

    fun normalizeLaserPower(laserPower: String): String {
        return normalizeIntInRange(
            value = laserPower,
            min = -100,
            max = 0,
            defaultValue = ConfigTestModeBean.DEFAULT_LASER_POWER
        )
    }

    fun applyConfig(
        config: CardConfig,
        absorbTimeMillis: String,
        reactionTimeSeconds: String,
        laserPower: String,
    ): CardConfig {
        config.xt1 = (normalizeAbsorbTimeMillis(absorbTimeMillis).toInt() + 999) / 1000
        config.cutOff1 = normalizeLaserPower(laserPower).toDouble()
        config.cutOff2 = normalizeReactionTimeSeconds(reactionTimeSeconds).toDouble()
        return config
    }

    fun buildLocalCardInfo(
        cardCode: String,
        config: ConfigTestModeBean,
    ): CardInfoBean {
        val cardBatchCode = AppTypeUtils.findCardBatchCode(cardCode)
        val cardConfig = applyConfig(
            config = CardConfig(),
            absorbTimeMillis = config.absorbTimeMillis,
            reactionTimeSeconds = config.reactionTimeSeconds,
            laserPower = config.laserPower
        )
        return CardInfoBean(
            card = Card(
                id = AppTypeUtils.findCardId(cardCode),
                cardBatchId = cardBatchCode,
                code = cardCode,
                status = CardStatus.ACTIVE.statusVal,
            ),
            cardBatch = CardBatch(
                id = cardBatchCode,
                code = cardBatchCode,
                type = CaseBean.TYPE_BIOAGE_CRP,
            ),
            cardConfig = cardConfig,
        )
    }

    private fun normalizeIntInRange(
        value: String,
        min: Int,
        max: Int,
        defaultValue: String,
    ): String {
        val parsed = value.trim().toIntOrNull()
        return if (parsed != null && parsed in min..max) {
            parsed.toString()
        } else {
            defaultValue
        }
    }
}
