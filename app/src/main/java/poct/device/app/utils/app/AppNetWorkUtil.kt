package poct.device.app.utils.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

/**
 * *
 *
 *
 * @desc：网络帕努单 相关工具类，部分方法已过期 需重写
 */
object AppNetWorkUtil {

    /**
     * 判断网络是否处于连接状态
     */
    fun isConnected(context: Context): Boolean {
        val info = getActiveNetworkInfo(context)
        return info?.isConnected ?: false
    }

    /**
     * 获取网络连接信息
     */
    private fun getActiveNetworkInfo(context: Context): NetworkInfo? {
        val cm = context
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo
    }
}