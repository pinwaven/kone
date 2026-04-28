package poct.device.app.serial.v2.ctl

import io.netty.channel.ChannelHandlerContext
import poct.device.app.serial.v2.CtlSerialMessageV2
import poct.device.app.serial.v2.common.event.AbstractSocketMessageReceivedEventV2

class CtlSerialMessageEventV2(
    uid: String?,
    message: CtlSerialMessageV2?,
    context: ChannelHandlerContext?,
) : AbstractSocketMessageReceivedEventV2<CtlSerialMessageV2>(uid, message, context) {

}