package poct.device.app.bean

data class ConfigInnerBean(
    var kdbs: String = "",
    var rdss: String = "",
    var rbc: String = "",
    var ydbs: String = "",
    var shbs: String = "",
    var ft: String = "",
    var xt: String = "",
    var zxfw: String = "",
    var tkbs: String = "",
) : ConfigBean {
    companion object {
        const val PREFIX = "inner_"
        val Empty = ConfigInnerBean()
    }
}
