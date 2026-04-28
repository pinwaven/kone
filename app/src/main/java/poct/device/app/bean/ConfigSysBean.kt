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

) : ConfigBean {
    companion object {
        const val PREFIX = "sys_"
        val Empty = ConfigSysBean()
    }
}
