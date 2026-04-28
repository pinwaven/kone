package poct.device.app.serial.ctl

import info.szyh.socket.event.AbstractSocketMessageReceivedEvent
import io.netty.channel.ChannelHandlerContext


class CtlSerialMessageEvent(
    uid: String?,
    message: CtlSerialMessage?,
    context: ChannelHandlerContext?,
) : AbstractSocketMessageReceivedEvent<CtlSerialMessage>(uid, message, context) {

}