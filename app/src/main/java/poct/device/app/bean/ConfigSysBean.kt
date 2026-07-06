package poct.device.app.bean

data class ConfigSysBean(
    var lang: String = "cn",
    var scan: String = "y", // 是否扫码 y/n
    var sensorDetection: String = "y", // 传感器检测 y/n，空视为开启

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
    var nanoDeviceId: String = "",  // matches nano kino_devices.serial_number

) : ConfigBean {
    companion object {
        const val PREFIX = "sys_"
        const val FLOW_CLINICAL = "clinical"
        const val FLOW_NANO = "nano"
        val Empty = ConfigSysBean()

        fun defaultFlow(flow: String): String =
            flow.ifBlank { FLOW_NANO }

        fun isNanoFlow(flow: String): Boolean =
            defaultFlow(flow) == FLOW_NANO

        fun isSensorDetectionEnabled(sensorDetection: String): Boolean =
            sensorDetection != "n"
    }
}
