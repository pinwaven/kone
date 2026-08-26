package poct.device.app.bean

data class ConfigLaserBean(
    var power: String = "",
) : ConfigBean {
    companion object {
        const val PREFIX = "laser_"
    }
}
