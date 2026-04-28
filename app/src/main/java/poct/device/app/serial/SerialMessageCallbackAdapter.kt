package poct.device.app.serial

import info.szyh.socket.comm.SocketMessageSender
import info.szyh.socket.handler.SocketMessage
import kotlinx.coroutines.CoroutineScope

abstract class SerialMessageCallbackAdapter<T : SocketMessage?> : SerialMessageCallback<T> {
    override suspend fun beforeTry(sender: SocketMessageSender<T>?, scope: CoroutineScope) {

    }

    override suspend fun afterTry(sender: SocketMessageSender<T>?, scope: CoroutineScope) {
    }

    override suspend fun delay(
        feedback: T,
        sender: SocketMessageSender<T>?,
        scope: CoroutineScope,
    ) {
    }

    override suspend fun success(
        feedback: T,
        sender: SocketMessageSender<T>?,
        scope: CoroutineScope,
    ) {
    }

    override suspend fun error(
        sender: SocketMessageSender<T>?,
        e: Exception?,
        scope: CoroutineScope,
    ) {
    }

    override suspend fun complete(sender: SocketMessageSender<T>?, scope: CoroutineScope) {
    }
}