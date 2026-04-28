package poct.device.app.utils.app

import android.view.Gravity
import android.widget.Toast
import poct.device.app.App
import poct.device.app.R

/**
 * *
 *
 *
 * @desc：toast 工具类
 */
object AppToastUtil {

    /**
     * 短提示
     * @param message 提示语
     */
    fun shortShow(message: String) {
        val toast = Toast.makeText(
            App.getContext(),
            message, Toast.LENGTH_SHORT
        )
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
        toast.show()
    }

    /**
     * 长提示
     * @param message 提示语
     */
    fun longShow(message: String) {
        val toast = Toast.makeText(
            App.getContext(),
            message, Toast.LENGTH_LONG
        )
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0)
        toast.show()
    }

    fun devShow() {
        shortShow(App.getContext().getString(R.string.developing))
    }

    fun roleShow() {
        shortShow(App.getContext().getString(R.string.user_permission))
    }

}