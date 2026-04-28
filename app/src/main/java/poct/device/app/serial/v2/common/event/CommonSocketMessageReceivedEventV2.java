package poct.device.app.serial.v2.common.event;

import io.netty.channel.ChannelHandlerContext;
import poct.device.app.serial.v2.common.SocketMessageV2;

public class CommonSocketMessageReceivedEventV2 extends AbstractSocketMessageReceivedEventV2<SocketMessageV2> {
    public CommonSocketMessageReceivedEventV2(String uid, SocketMessageV2 message, ChannelHandlerContext context) {
        super(uid, message, context);
    }
}

