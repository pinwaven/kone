package poct.device.app.utils.app

import poct.device.app.bean.CaseBean

object AppExperimentUtils {
    private val typeMap: Map<String, String> = mapOf(
        "1" to CaseBean.TYPE_4LJ,
        "2" to CaseBean.TYPE_2LJ_A,
        "3" to CaseBean.TYPE_3LJ,
        "4" to CaseBean.TYPE_2LJ_B,
    )

    /**
     * 获取实验类型
     */
    fun getType(qrCode: String): String? {
        return typeMap[qrCode]
    }
}