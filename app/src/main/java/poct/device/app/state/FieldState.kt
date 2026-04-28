package poct.device.app.state

import androidx.compose.ui.graphics.Color
import poct.device.app.theme.dangerColor
import poct.device.app.theme.warningColor

/**
 * *
 * @date ：2024/4/14
 * @desc：字段状态
 */
class FieldState(
    val name: String,
    val state: Int = STATE_NORMAL,
    val msg: String = "",
) {

    val color: Color?
        get() {
            return when (state) {
                STATE_WARNING -> warningColor
                STATE_ERROR -> dangerColor
                else -> null
            }
        }

    companion object {
        const val STATE_NORMAL = 0
        const val STATE_WARNING = 1
        const val STATE_ERROR = 2

        fun normal(name: String, msg: String): FieldState {
            return FieldState(name, STATE_NORMAL, msg)
        }

        fun error(name: String, msg: String): FieldState {
            return FieldState(name, STATE_ERROR, msg)
        }

        fun warning(name: String, msg: String): FieldState {
            return FieldState(name, STATE_WARNING, msg)
        }
    }
}
