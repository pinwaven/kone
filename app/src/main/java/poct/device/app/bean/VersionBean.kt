package poct.device.app.bean

data class VersionBean(
    var id: String,
    var version: String = "",
    var software: String = "",
    var softwareRemark: String = "",
    var hardware: String = "",
    var hardwareRemark: String = "",
    var lapseTime: String = "", // 过期日期 yyyyMMddHHmmss
    var handleTime: String = "", // 处理日期 yyyyMMddHHmmss
    var state: Int = 0,// 0-未处理 1-已取消 2-已完成 3-已失败 4-已过期, 和CommEnsTaskStatus相同
) {
    companion object {
        val Empty = VersionBean("")
    }
}
