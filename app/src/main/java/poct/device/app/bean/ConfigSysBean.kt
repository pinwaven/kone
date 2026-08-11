package poct.device.app.bean

data class ConfigSysBean(
    var lang: String = "cn",
    var scan: String = "y", // 是否扫码 y/n

    var jctm: String = "", // 检测条码: y/n
    var sys: String = "", // 实验室名称
    var checkMethod: String = "whole", // 检测方式
    var ige: String = "", // IGE扫码类型
    var crp: String = "", // CRP扫码类型
    var sf: String = "", // SF/CRP扫码类型
    var slj: String = "", // 4LJ扫码类型

    // Nano AI flow toggle. Empty is treated as the default Nano flow for
    // devices that have not persisted this setting yet.
    var flow: String = FLOW_NANO,

    // 空卡校准开关：y/n，默认关闭（n）
    var emptyCardCalibration: String = TOGGLE_OFF,

) : ConfigBean {
    companion object {
        const val PREFIX = "sys_"
        const val FLOW_CLINICAL = "clinical"
        const val FLOW_NANO = "nano"
        const val TOGGLE_ON = "y"
        const val TOGGLE_OFF = "n"
        val Empty = ConfigSysBean()

        fun defaultFlow(flow: String): String =
            flow.ifBlank { FLOW_NANO }

        fun isNanoFlow(flow: String): Boolean =
            defaultFlow(flow) == FLOW_NANO

        fun isEmptyCardCalibrationOn(value: String): Boolean = value == TOGGLE_ON
    }
}
