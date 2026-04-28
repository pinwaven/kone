package poct.device.app.serial.v2.common.event;

import io.netty.channel.ChannelHandlerContext;
import poct.device.app.serial.v2.common.SocketMessageV2;

public abstract class AbstractSocketMessageReceivedEventV2<T extends SocketMessageV2> {
    private final String uid;

    public String getUid() {
        return this.uid;
    }

    private final T message;

    public T getMessage() {
        return this.message;
    }

    private final ChannelHandlerContext context;

    public ChannelHandlerContext getContext() {
        return this.context;
    }

    public AbstractSocketMessageReceivedEventV2(String uid, T message, ChannelHandlerContext context) {
        this.uid = uid;
        this.message = message;
        this.context = context;
    }
}
