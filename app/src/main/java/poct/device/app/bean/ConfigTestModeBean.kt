package poct.device.app.bean

data class ConfigTestModeBean(
    var reactionTimeSeconds: String = DEFAULT_REACTION_TIME_SECONDS,
    var absorbTimeMillis: String = DEFAULT_ABSORB_TIME_MILLIS,
    var scanTimeMillis: String = DEFAULT_SCAN_TIME_MILLIS,
    var laserPower: String = DEFAULT_LASER_POWER,
) : ConfigBean {
    companion object {
        const val PREFIX = "test_mode_"
        const val DEFAULT_REACTION_TIME_SECONDS = "300"
        const val DEFAULT_ABSORB_TIME_MILLIS = "240000"
        const val DEFAULT_SCAN_TIME_MILLIS = "16000"
        const val DEFAULT_LASER_POWER = "-25"
        val Empty = ConfigTestModeBean()
    }
}
