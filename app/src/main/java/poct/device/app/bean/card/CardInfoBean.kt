package poct.device.app.bean.card

import poct.device.app.thirdparty.model.sbedge.resp.CardConfigTop
import poct.device.app.thirdparty.model.sbedge.resp.CardConfigVar
import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalDateTime

enum class CardStatus(val statusVal: String) {
    ACTIVE("active"),
    CHECKING("checking"),
    FAIL("fail"),
    SUCCESS("success"),
    INACTIVE("inactive"),
}

data class CardInfoBean(
    var card: Card = Card.Empty,
    var cardBatch: CardBatch = CardBatch.Empty,
    var cardConfig: CardConfig = CardConfig.Empty,
) {
    companion object {
        val Empty = CardInfoBean()
    }
}

data class Card(
    var id: String = "",
    var cardBatchId: String = "",
    var code: String = "",
    var usedDate: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
    var status: String = "",
    var createdAt: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
    var updatedAt: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
) {
    companion object {
        val Empty = Card()
    }
}

data class CardBatch(
    var id: String = "",
    var name: String = "",
    var type: String = "",
    var code: String = "",
    var prodDate: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
    var expDate: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
    var status: String = "",
    var guideVideo: String? = null,
    var guideText: String? = null,
    var createdAt: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
    var updatedAt: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
) {
    companion object {
        val Empty = CardBatch()
    }
}

data class CardConfig(
    var scanStart: String = "",
    var scanEnd: String = "",
    var scanPPMM: Int = 0,
    var topList: List<CardConfigTop> = emptyList(),
    var varList: List<CardConfigVar> = emptyList(),
    var ft0: Int = 0, // 初始反应时间
    var xt1: Int = 0, // 吸水时间
    var ft1: Int = 0, // 反应时间
    var scope: Double = 0.0,
    var typeScore: Double = 0.0,
    var cAvg: Double = 0.0,
    var cStd: Double = 0.0,
    var cMin: Double = 0.0, // 参考值最小值
    var cMax: Double = 0.0,
    var cutOff1: Double = 0.0, // 激光功率
    var cutOff2: Double = 0.0, // 设备外反应时间
    var cutOff3: Double = 0.0,
    var cutOff4: Double = 0.0,
    var cutOff5: Double = 0.0,
    var cutOff6: Double = 0.0,
    var cutOff7: Double = 0.0,
    var cutOff8: Double = 0.0,
    var cutOffMax: Double = 0.0,
    var noise1: Double = 0.0,
    var noise2: Double = 0.0,
    var noise3: Double = 0.0,
    var noise4: Double = 0.0,
    var noise5: Double = 0.0,
) {
    companion object {
        val Empty = CardConfig()
    }
}
