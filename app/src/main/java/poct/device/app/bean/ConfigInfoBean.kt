package poct.device.app.bean

data class ConfigInfoBean(
    var name: String = "",
    var code: String = "",
    var type: String = "",
    var software: String = "",
    var hardware: String = "",
) : ConfigBean {
    companion object {
        const val PREFIX = "info_"
        val Empty = ConfigInfoBean()
    }
}
