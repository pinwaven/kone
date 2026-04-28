package poct.device.app.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import poct.device.app.AppParams
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MainMainViewModel : ViewModel() {
    val wifiConnected = MutableStateFlow(AppParams.wlanEnabled)

    /**
     * 检查网络是否实际可访问（用户是否授予了权限）
     */
    suspend fun isNetworkAccessible(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 尝试建立一个最简单的HTTP连接（到公认可达的地址）
                val url = URL("https://www.baidu.com")
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    requestMethod = "HEAD"
                }
                val responseCode = connection.responseCode
                Timber.w("homeMain isNetworkAccessible responseCode: $responseCode")
                connection.disconnect()
                // 如果连接成功（即使返回204无内容），也说明有权限
                true
            } catch (e: SocketTimeoutException) {
                // 连接超时，可能是网络慢，但不能断定无权限
                Timber.w("homeMain isNetworkAccessible SocketTimeoutException: ${e.message}")
                false
            } catch (e: IOException) {
                // 发生IO异常，很可能网络被完全阻断（包括被流量管控禁止）
                Timber.w("homeMain isNetworkAccessible IOException: ${e.message}")
                false
            }
        }
    }
}