package poct.device.app.serial

import info.szyh.socket.comm.SocketMessageSender
import info.szyh.socket.handler.SocketMessage
import kotlinx.coroutines.CoroutineScope

interface SerialMessageCallback<T : SocketMessage?> {
    /**
     * 尝试发送前调用
     *
     * @param sender
     */
    suspend fun beforeTry(sender: SocketMessageSender<T>?, scope: CoroutineScope)

    /**
     * 尝试发送后调用
     *
     * @param sender
     */
    suspend fun afterTry(sender: SocketMessageSender<T>?, scope: CoroutineScope)

    /**
     * delay响应
     *
     * @param result
     * @param sender
     */
    suspend fun delay(result: T, sender: SocketMessageSender<T>?, scope: CoroutineScope)

    /**
     * 接收到回调后，回调
     *
     * @param result
     * @param sender
     */
    suspend fun success(result: T, sender: SocketMessageSender<T>?, scope: CoroutineScope)

    /**
     * 异常回调
     *
     * @param sender
     * @param e
     */
    suspend fun error(sender: SocketMessageSender<T>?, e: Exception?, scope: CoroutineScope)

    /**
     * 完成，接收到回调，或者超时
     *
     * @param sender
     */
    suspend fun complete(sender: SocketMessageSender<T>?, scope: CoroutineScope)
}
