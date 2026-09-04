package poct.device.app.bean

/**
 * 主板欠压重启保护状态。pending/batteryAtPending 均以字符串存储，
 * 因 SysConfigService 反射映射仅支持 ConfigBean 的 String 属性。
 */
data class BoardPowerGuardBean(
    var pending: String = "false",
    var batteryAtPending: String = "-1",
) : ConfigBean {
    companion object {
        const val PREFIX = "board_power_guard_"
    }
}
