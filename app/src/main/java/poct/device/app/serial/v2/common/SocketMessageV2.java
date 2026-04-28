package poct.device.app.serial.v2.common;

import io.netty.buffer.ByteBuf;

public interface SocketMessageV2 {
    String getUid();

    int getSid();

    ByteBuf toByteBuf();
}
