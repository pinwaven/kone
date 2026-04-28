package poct.device.app.serial.v2

import kotlinx.coroutines.CoroutineScope
import poct.device.app.serial.v2.common.SocketMessageSenderV2
import poct.device.app.serial.v2.common.SocketMessageV2

interface SerialMessageCallbackV2<T : SocketMessageV2?> {
    /**
     * 尝试发送前调用
     *
     * @param sender
     */
    suspend fun beforeTry(sender: SocketMessageSenderV2<T>?, scope: CoroutineScope)

    /**
     * 尝试发送后调用
     *
     * @param sender
     */
    suspend fun afterTry(sender: SocketMessageSenderV2<T>?, scope: CoroutineScope)

    /**
     * delay响应
     *
     * @param result
     * @param sender
     */
    suspend fun delay(result: T, sender: SocketMessageSenderV2<T>?, scope: CoroutineScope)

    /**
     * 接收到回调后，回调
     *
     * @param result
     * @param sender
     */
    suspend fun success(result: T, sender: SocketMessageSenderV2<T>?, scope: CoroutineScope)

    /**
     * 异常回调
     *
     * @param sender
     * @param e
     */
    suspend fun error(sender: SocketMessageSenderV2<T>?, e: Exception?, scope: CoroutineScope)

    /**
     * 完成，接收到回调，或者超时
     *
     * @param sender
     */
    suspend fun complete(sender: SocketMessageSenderV2<T>?, scope: CoroutineScope)
}
