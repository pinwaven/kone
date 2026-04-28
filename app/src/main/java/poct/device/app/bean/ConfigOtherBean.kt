package poct.device.app.bean

data class ConfigOtherBean(
    var wlan: String = "n", // y or n
    var lang: String = "cn",
    var printer: String = "",
    var scanner: String = "",
) : ConfigBean {
    companion object {
        const val PREFIX = "other_"
        val Empty = ConfigOtherBean()
    }
}
