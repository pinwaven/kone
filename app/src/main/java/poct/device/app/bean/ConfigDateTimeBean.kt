package poct.device.app.bean

import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalDateTime

data class ConfigDateTimeBean(
    var timeSync: String = "n", // y or n
    var timeZone: String = "",
    var time: String = AppLocalDateUtils.formatDateTime(LocalDateTime.now()),
) : ConfigBean {
    companion object {
        val Empty = ConfigDateTimeBean()
    }
}
