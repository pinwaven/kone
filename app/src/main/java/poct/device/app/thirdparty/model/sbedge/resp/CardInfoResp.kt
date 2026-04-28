package poct.device.app.thirdparty.model.sbedge.resp

class CardInfoResp(
    var card: Card,
    var cardBatch: CardBatch,
    var cardConfig: CardConfig?,
)

class Card(
    var id: String,
    var cardBatchId: String,
    var code: String,
    var usedDate: String,
    var status: String,
    var createdAt: String,
    var updatedAt: String,
)

class CardBatch(
    var id: String,
    var name: String,
    var type: String,
    var code: String,
    var prodDate: String,
    var expDate: String,
    var status: String,
    var guideVideo: String,
    var guideText: String,
    var createdAt: String,
    var updatedAt: String,
)

class CardConfig(
    var scanStart: String,
    var scanEnd: String,
    var scanPpmm: Int,
    var topList: List<CardConfigTop>,
    var varList: List<CardConfigVar>,
    var ft0: Int,
    var xt1: Int,
    var ft1: Int,
    var scope: Double,
    var typeScore: Double,
    var cAvg: Double,
    var cStd: Double,
    var cMin: Double, // 参考值最小值
    var cMax: Double,
    var cutOff1: Double, // 激光功率
    var cutOff2: Double, // 设备外反应时间
    var cutOff3: Double,
    var cutOff4: Double,
    var cutOff5: Double,
    var cutOff6: Double,
    var cutOff7: Double,
    var cutOff8: Double,
    var cutOffMax: Double,
    var noise1: Double,
    var noise2: Double,
    var noise3: Double,
    var noise4: Double,
    var noise5: Double,
)

class CardConfigTop(
    var id: String,
    var start: Double,
    var end: Double,
    var ctrl: String,
    var name: String,
)

class CardConfigVar(
    var id: String,
    var type: String,
    var start: Double,
    var end: Double,
    var x0: Double,
    var x1: Double,
    var x2: Double,
    var x3: Double,
    var x4: Double,
)