package poct.device.app.bean

import kotlinx.serialization.json.Json
import poct.device.app.bean.card.CardInfoBean
import poct.device.app.entity.CasePoint
import poct.device.app.entity.CaseResult
import poct.device.app.thirdparty.model.sbedge.resp.Assets
import poct.device.app.thirdparty.model.sbedge.resp.BaaResult
import poct.device.app.utils.app.AppFileUtils
import poct.device.app.utils.app.AppLocalDateUtils
import timber.log.Timber
import java.time.LocalDateTime
import java.util.Collections

data class CaseBean(
    var id: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),

    // TODO 简化信息
    var patientId: String = "", // Jc7F5yTz0N

    /**
     * 姓名
     */
    var name: String = "User",

    /**
     * 性别，1男2女
     */
    var gender: Int = 1,

    /**
     * 生日， YYYY-MM-dd
     */
    var birthday: String = "",

    /**
     * 样本ID
     */
    var caseId: String = "",

    /**
     * 样本类型
     */
    var caseType: Int = 1,

    /**
     * 芯片二维码数据
     */
    var qrCode: String = "",

    /**
     * 试剂ID(读卡获取)
     */
    var reagentId: String = "",

    /**
     * 检测项目
     */
    var type: String = "ige",

    /**
     * 检测时间
     */
    var workTime: String = "",

    /**
     * 检测结果, List<CaseResult>
     */
    var workResult: String = "",

    /**
     * 样本状态：根据参考值判断
     */
    var state: Int = 0,

    /**
     * 点位数值, List<Int>
     */
    var workPoints: String = "",

    /**
     * 试剂卡信息
     */
    var cardInfo: CardInfoBean = CardInfoBean.Empty,

    /**
     * 抗衰结果
     */
    var baaResult: BaaResult = BaaResult.Empty,

    /**
     * 抗衰图片
     */
    var baaAssets: Assets = Assets.Empty
) {
    val pdfPath: String
        get() {
            return "${AppFileUtils.getInnerRoot()}/pdf/$id.pdf"
        }

    val resultList: List<CaseResult>
        get() {
            return try {
                Json.decodeFromString(workResult)
            } catch (e: Exception) {
                Timber.tag("JSON").e(e.message ?: "JSON parser error")
                Collections.emptyList()
            }
        }


    val pointList: List<CasePoint>
        get() {
            return try {
                Json.decodeFromString(workPoints)
            } catch (e: Exception) {
                Timber.tag("JSON").e(e.message ?: "JSON parser error")
                Collections.emptyList()
            }
        }

    companion object {
        val Empty = CaseBean()
        const val TYPE_IGE = "ige"
        const val TYPE_CRP = "crp"
        const val TYPE_4LJ = "4lj"
        const val TYPE_3LJ = "3lj"
        const val TYPE_SF = "sf/crp"
        const val TYPE_2LJ_A = "2lj_a"
        const val TYPE_2LJ_B = "2lj_b"

        const val TYPE_BIOAGE_CRP = "bioage_crp"
        const val TYPE_2LJ_B_M = "2lj_b_m"
        const val TYPE_2LJ_B_F = "2lj_b_f"
        const val TYPE_3LJ_BIOAGE_L1 = "3lj_bioage_l1"

        const val IGE_T1 = "Total-IgE"
        const val CRP_T1 = "CRP"
        const val CRP_T2 = "SF"

        /**
         * T1是合胞病毒 RSV
         * T2是甲流  FluA
         * T3是乙流  FluB
         * T4是新冠  SARS
         */
        const val _4LJ_T1 = "RSV"
        const val _4LJ_T2 = "FluA"
        const val _4LJ_T3 = "FluB"
        const val _4LJ_T4 = "SARS"

        const val _3LJ_T1 = "FluA"
        const val _3LJ_T2 = "FluB"
        const val _3LJ_T3 = "SARS"

        const val _2LJ_A_T1 = "CRP"
        const val _2LJ_A_T2 = "铁蛋白"

        const val _2LJ_B_T1 = "糖化血红蛋白"
        const val _2LJ_B_T2 = "胱抑素C"

//        const val _3LJ_BIOAGE_L1_T1 = "胱抑素C"
//        const val _3LJ_BIOAGE_L1_T2 = "CRP"
//        const val _3LJ_BIOAGE_L1_T3 = "糖化血红蛋白"

        const val _2LJ_B_M_T1 = "生理年龄"
        const val _2LJ_B_F_T1 = "生理年龄"

        const val _3LJ_BIOAGE_L1_MAIN_Final = "生理年龄"

        //        const val _3LJ_BIOAGE_L1_MAIN_T1 = "CystatinC"
        const val _3LJ_BIOAGE_L1_MAIN_T2 = "hsCRP"
    }
}

