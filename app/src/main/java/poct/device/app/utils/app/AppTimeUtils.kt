package poct.device.app.utils.app

import android.content.Context
import android.provider.Settings
import poct.device.app.App
import timber.log.Timber
import java.time.LocalDateTime


object AppTimeUtils {
    fun setDateTime(dateTime: LocalDateTime) {
        AppSystemUtils.setTime(dateTime)
    }

    fun setAutoDateTime(context: Context, checked: Int) {
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.AUTO_TIME, checked
        )
    }

    private fun checkDateAutoSet(): Boolean {
        val context: Context = App.getContext()
        val isAutoSet: Boolean = try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AUTO_TIME
            ) > 0
        } catch (exception: Exception) {
            Timber.tag("time").e(exception.stackTraceToString())
            false
        }
        return isAutoSet
    }

    fun setTimeZone(timeZone: String) {
        AppSystemUtils.setTimeZone(timeZone)
    }

    fun setAutoTimeZone(checked: Boolean) {
        AppSystemUtils.setTimeAuto(checked)
    }
}