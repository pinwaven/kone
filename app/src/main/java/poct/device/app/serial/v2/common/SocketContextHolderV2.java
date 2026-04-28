package poct.device.app.serial.v2.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;

public class SocketContextHolderV2 {
    private final Map<String, ChannelHandlerContext> contextMap = new ConcurrentHashMap<>();

    public SocketContextHolderV2() {
    }

    public void put(String uid, ChannelHandlerContext context) {
        this.contextMap.put(uid, context);
    }

    public List<ChannelHandlerContext> getAllContext() {
        return new ArrayList<>(this.contextMap.values());
    }

    public List<String> getAllUids() {
        return new ArrayList<>(this.contextMap.keySet());
    }

    public ChannelHandlerContext getContext(String uid) {
        return this.contextMap.get(uid);
    }

    public String getUid(ChannelHandlerContext context) {
        for (Map.Entry<String, ChannelHandlerContext> entry : this.contextMap.entrySet()) {
            if (entry.getValue().equals(context)) {
                return entry.getKey();
            }
        }

        return null;
    }

    public void removeByUid(String code) {
        this.contextMap.remove(code);
    }

    public void removeByContext(ChannelHandlerContext context) {
        String code = this.getUid(context);
        if (code != null) {
            this.removeByUid(code);
        }
    }
}
