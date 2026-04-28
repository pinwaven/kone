package poct.device.app.serial.v2

import kotlinx.coroutines.CoroutineScope
import poct.device.app.serial.v2.common.SocketMessageSenderV2
import poct.device.app.serial.v2.common.SocketMessageV2

abstract class SerialMessageCallbackAdapterV2<T : SocketMessageV2?> : SerialMessageCallbackV2<T> {
    override suspend fun beforeTry(sender: SocketMessageSenderV2<T>?, scope: CoroutineScope) {

    }

    override suspend fun afterTry(sender: SocketMessageSenderV2<T>?, scope: CoroutineScope) {
    }

    override suspend fun delay(
        feedback: T,
        sender: SocketMessageSenderV2<T>?,
        scope: CoroutineScope,
    ) {
    }

    override suspend fun success(
        feedback: T,
        sender: SocketMessageSenderV2<T>?,
        scope: CoroutineScope,
    ) {
    }

    override suspend fun error(
        sender: SocketMessageSenderV2<T>?,
        e: Exception?,
        scope: CoroutineScope,
    ) {
    }

    override suspend fun complete(sender: SocketMessageSenderV2<T>?, scope: CoroutineScope) {
    }
}