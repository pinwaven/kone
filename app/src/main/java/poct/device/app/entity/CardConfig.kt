package poct.device.app.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import poct.device.app.bean.CaseBean
import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalDateTime

/**
 * IgE: xt1, ft1,cAvg，cStd,cMin,cMax
 * 4LJ: xt1, ft1,cAvg，cStd,cMin,cMax,cutOff1,cutOff2,cutOff3,cutOff4
 */
@Entity(tableName = "tbl_card_config")
data class CardConfig(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var code: String = "",
    var type: String = CaseBean.TYPE_IGE,
    // 出厂时间
    var prodDate: String = "",
    // 过期时间
    var endDate: String = "",
    // 扫描开始位置 mm
    var scanStart: Double = 0.0,
    // 扫描结束位置 mm
    var scanEnd: Double = 0.0,
    // 扫描密度：1mm扫描点数
    var scanPPMM: Int = 40,

    var topList: String = "[]",
    var varList: String = "[]",

    var gmtCreated: LocalDateTime = LocalDateTime.now(),
    var gmtModified: LocalDateTime = LocalDateTime.now(),

    // ===== 以下根据不同不同项目，配置不一样 =====
    // 初始反应时间
    var ft0: Int = 0,
    // 吸水时间（秒）
    var xt1: Int = 0,
    // 反应时间（秒）
    var ft1: Int = 0,
    // 斜率校验
    var scope: Double = 0.0,
    /**
     * 血型系数，全血类型是系数，默认1.0
     */
    var typeScore: Double = 1.0,
    // 参考值平均值
    var cAvg: Double = 0.0,
    // 参考值标准差
    var cStd: Double = 0.0,
    // 参考值最小值
    var cMin: Double = 0.0,
    // 参考值最大值
    var cMax: Double = 0.0,
    // 激光功率
    var cutOff1: Double = 0.0,
    // 设备外反应时间
    var cutOff2: Double = 0.0,
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
    var createdAt: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
    var updatedAt: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
) {
    companion object {
        val EMPTY = CardConfig()
    }
}

@Serializable
data class CardTop(
    var index: Int = 0,
    var id: String = "",
    var start: Double = 0.0,
    var end: Double = 0.0,
    // y or n
    var ctrl: String = "y",
) {
    companion object {
        val Empty = CardTop()
    }
}

@Serializable
data class CardVar(
    var index: Int = 0,
    var id: String = "",
    var type: String = "crp",
    var start: Double = 0.0,
    var end: Double = 0.0,
    var x0: Double = 0.0,
    var x1: Double = 0.0,
    var x2: Double = 0.0,
    var x3: Double = 0.0,
    var x4: Double = 0.0,
) {
    companion object {
        val Empty = CardVar()
    }
}
