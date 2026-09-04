package poct.device.app.bean

data class ConfigNanoAuthBean(
    var rootToken: String = "",
    var commToken: String = "",
    var commTokenExpiresAt: String = "",
    var machineNo: String = "",
    var machineName: String = "",
    var model: String = "",
    var status: String = "",
    var activatedAt: String = "",
    var refreshedAt: String = "",
    var firmwareId: String = "",
) : ConfigBean {
    companion object {
        const val PREFIX = "nano_auth_"
        val Empty = ConfigNanoAuthBean()
    }
}
