package poct.device.app.bean

import kotlinx.serialization.Serializable
import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalDateTime
import java.util.Collections

data class CardConfigBean(
    var showDetail: Boolean = false,
    var id: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
    var name: String = "",
    var type: String = "", // CaseBean.TYPE_IGE,
    var code: String = "",
    var prodDate: String = "",
    var endDate: String = "",
    // 扫描开始位置 mm
    var scanStart: String = "",
    // 扫描结束位置 mm
    var scanEnd: String = "",
    // 扫描密度：1mm扫描点数
    var scanPPMM: String = "",
    var topList: List<CardTopBean> = Collections.emptyList(),
    var varList: List<CardVarBean> = Collections.emptyList(),
    var ft0: String = "",
    var xt1: String = "",
    var ft1: String = "",
    /**
     * 血型系数，全血类型是系数，默认1.0
     */
    var typeScore: String = "",
    // 参考值平均值
    var cAvg: String = "",
    // 参考值标准差
    var cStd: String = "",
    // 参考值最小值
    var cMin: String = "",
    // 参考值最大值
    var cMax: String = "",
    // 激光功率
    var cutOff1: String = "",
    // 设备外反应时间
    var cutOff2: String = "",
    var cutOff3: String = "",
    var cutOff4: String = "",

    var cutOff5: String = "",
    var cutOff6: String = "",
    var cutOff7: String = "",
    var cutOff8: String = "",

    var cutOffMax: String = "",

    var noise1: String = "",
    var noise2: String = "",
    var noise3: String = "",
    var noise4: String = "",
    var noise5: String = "",

    var scope: String = "",
) {
    companion object {
        val Empty = CardConfigBean()
    }
}

@Serializable
data class CardTopBean(
    var index: Int = 0,
    var id: String = "",
    var start: String = "0.0",
    var end: String = "0.0",
    // y or n
    var ctrl: String = "y",
) {
    companion object {
        val Empty = CardTopBean()
    }
}

@Serializable
data class CardVarBean(
    var index: Int = 0,
    var id: String = "",
    var type: String = "crp",
    var start: String = "0.0",
    var end: String = "0.0",
    var x0: String = "0.0",
    var x1: String = "0.0",
    var x2: String = "0.0",
    var x3: String = "0.0",
    var x4: String = "0.0",
) {
    companion object {
        val Empty = CardVarBean()
    }
}
