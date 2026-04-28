package poct.device.app.utils.app

import poct.device.app.bean.CaseBean
import poct.device.app.bean.ConfigSysBean

/**
 * 检测项目帮助类
 */
object AppTypeUtils {
    /**
     * 从芯片二维码查找项目类型
     */
    fun findType(qrCode: String, sysConfig: ConfigSysBean?): String? {
        if (sysConfig == null) {
            return null
        }
        if (qrCode.length < 18) {
            return null
        }
        val code = qrCode.substring(4, 6)
        return if (code == sysConfig.ige) {
            CaseBean.TYPE_IGE
        } else if (code == sysConfig.slj) {
            CaseBean.TYPE_4LJ
        } else if (code == sysConfig.crp) {
            CaseBean.TYPE_CRP
        } else if (code == sysConfig.sf) {
            CaseBean.TYPE_SF
        } else {
            null
        }
    }

    /**
     * 从芯片二维码查找项目类型
     */
    fun findTypeV2(qrCode: String): String? {
        if (qrCode.length < 6) {
            return null
        }

        val code = qrCode.substring(4, 6)
        return when (code) {
            "01" -> {
                CaseBean.TYPE_4LJ
            }

            "05" -> {
                CaseBean.TYPE_2LJ_B_M
            }

            "06" -> {
                CaseBean.TYPE_2LJ_B_F
            }

            "07" -> {
                CaseBean.TYPE_3LJ_BIOAGE_L1
            }

            "08" -> {
                CaseBean.TYPE_BIOAGE_CRP
            }

            else -> {
                null
            }
        }
    }

    /**
     * 从芯片二维码查找批次
     */
    fun findReagentId(qrCode: String): String {
        val split = qrCode.split("-")
        return split[0]
    }

    /**
     * 从芯片二维码查找批次
     */
    fun findCardBatchCode(qrCode: String): String {
        val split = qrCode.split("-")
        return split[0]
    }

    /**
     * 从芯片二维码查找批次
     */
    fun findCardId(qrCode: String): String {
        val split = qrCode.split("-")
        return split[split.size - 1]
    }

    /**
     * 从芯片二维码查找批次
     */
    fun findCardCode(qrCode: String): String {
        val split = qrCode.split("-")
        return split[split.size - 1]
    }
}