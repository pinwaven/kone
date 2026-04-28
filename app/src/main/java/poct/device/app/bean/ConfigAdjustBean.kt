package poct.device.app.bean

data class ConfigAdjustBean(
    var jgName: String = "",  // 激光强度校准
    var posName1: String = "", // 移除片仓位置校准
    var posName2: String = "",  // 吸水位置校准
    var posName3: String = "",  // 扫描起始位置校准
    var jcName1: String = "", // 检测器增益

) : ConfigBean {
    companion object {
        const val PREFIX = "adjust_"
        val Empty = ConfigAdjustBean()
    }
}
