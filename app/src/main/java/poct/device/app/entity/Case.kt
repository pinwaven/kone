package poct.device.app.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * id为UUID
 */
@Entity(tableName = "tbl_case")
data class Case(
    /**
     * 检测编号
     */
    @PrimaryKey var id: String,

    /**
     * 姓名
     */
    var name: String,

    /**
     * 性别，1男2女
     */
    var gender: Int,

    /**
     * 生日， YYYY-MM-dd
     */
    var birthday: LocalDate,

    /**
     * 芯片二维码数据
     */
    var qrCode: String = "",

    /**
     * 检测编号
     */
    var caseId: String,

    /**
     * 样本类型，1血浆/血清2全血
     */
    var caseType: Int = 1,

    /**
     * 试剂ID
     */
    var reagentId: String,

    /**
     * 检测时间
     */
    var time: LocalDateTime,

    /**
     * 检测项目
     */
    var type: String,

    /**
     * 样本状态：根据参考值判断
     *  0-结果正常 1-结果异常/结果偏小 2-结果偏大
     */
    var state: Int,

    /**
     * JSON,CaseResult列表
     */
    var result: String,

    /**
     * JSON，CasePoint列表
     */
    var points: String,

    var gmtCreated: LocalDateTime,

    var gmtModified: LocalDateTime,
)

@Serializable
data class CaseResult(
    /**
     * 检测项目 - 这里是指具体的项目
     */
    var name: String,
    /**
     * 检测结果
     */
    var result: String,
    /**
     * 结果比值
     */
    var radioValue: String = "0.0",
    /**
     * 结果范围
     */
    var refer: String,
    /**
     * 结果属性，是否异常 0-正常， 1-异常
     */
    var flag: Int = 0,
    /**
     * T1
     */
    var t1Value: String,
    /**
     * T2
     */
    var t2Value: String = "0.0",
    /**
     * T3
     */
    var t3Value: String = "0.0",
    /**
     * T4
     */
    var t4Value: String = "0.0",

    /**
     * T1 Name
     */
    var t1ValueName: String = "",
    /**
     * T2 Name
     */
    var t2ValueName: String = "",
    /**
     * T3 Name
     */
    var t3ValueName: String = "",
    /**
     * T4 Name
     */
    var t4ValueName: String = "",

    /**
     * T1 Str
     */
    var t1ValueStr: String = "",
    /**
     * T2 Str
     */
    var t2ValueStr: String = "",
    /**
     * T3 Str
     */
    var t3ValueStr: String = "",
    /**
     * T4 Str
     */
    var t4ValueStr: String = "",

    /**
     * C1V
     */
    var cValue: String,

    /**
     * C2V
     */
    var c2Value: String = "0.0"
)

@Serializable
data class CasePoint(
    var x: Double,
    var y: Double,
)

