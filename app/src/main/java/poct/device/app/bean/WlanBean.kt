package poct.device.app.bean

data class WlanBean(
    /**
     * 名称
     */
    var ssid: String = "",
    var bssid: String = "",
    var capabilities: String = "",
    var signalStrength: Int = 0,
    /**
     * 连接状态
     */
    var connected: Boolean = false,

    ) {

    val hasPassword: Boolean
        get() {
            return (capabilities.contains("WEP")
                    || capabilities.contains("WPA")
                    || capabilities.contains("WPA2")
                    || capabilities.contains("WPA3")
                    || capabilities.contains("WPS")
                    || capabilities.contains("EAP")
                    || capabilities.contains("802.1x"))
        }

    val passwordType: String
        get() {
            if (capabilities.uppercase().contains("WEP")) {
                return "WEP"
            } else if (capabilities.uppercase().contains("WPA")) {
                return "WPA"
            } else {
                return ""
            }
        }

    companion object {
        val Empty = WlanBean()
    }
}
