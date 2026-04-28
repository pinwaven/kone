package poct.device.app.serial.v2.utils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import io.netty.channel.ChannelHandlerContext;
import poct.device.app.serial.v2.CtlSerialMessageV2;
import poct.device.app.serial.v2.common.SocketMessageSenderHolderV2;
import poct.device.app.serial.v2.common.SocketMessageSenderV2;
import poct.device.app.serial.v2.common.SocketMessageV2;
import poct.device.app.serial.v2.common.event.AbstractSocketMessageReceivedEventV2;
import poct.device.app.serial.v2.common.event.CommonSocketMessageReceivedEventV2;
import poct.device.app.utils.common.ReflectUtils;

public final class SocketCommUtils {
    private static final String TAG = "IOTPlugin-" + SocketCommUtils.class.getSimpleName();

    private static final Map<Class<? extends SocketMessageV2>, Class<? extends AbstractSocketMessageReceivedEventV2<? extends SocketMessageV2>>> EVENT_RECEIVED_CLASS_MAP = new HashMap<>(4);

    private SocketCommUtils() {
    }

    public static void registerReceivedEventClass(Class<? extends SocketMessageV2> msgClazz, Class<? extends AbstractSocketMessageReceivedEventV2<? extends SocketMessageV2>> receivedEventClass) {
        EVENT_RECEIVED_CLASS_MAP.put(msgClazz, receivedEventClass);
    }

    private static <E extends SocketMessageV2> AbstractSocketMessageReceivedEventV2<? extends SocketMessageV2> createReceivedEvent(String uid, E sockMsg, ChannelHandlerContext context) {
        Class<? extends AbstractSocketMessageReceivedEventV2<? extends SocketMessageV2>> eventClazz = EVENT_RECEIVED_CLASS_MAP.get(sockMsg.getClass());
        if (eventClazz == null) {
            eventClazz = CommonSocketMessageReceivedEventV2.class;
        }

        return ReflectUtils.newObject(eventClazz, uid, sockMsg, context);
    }

    public static <E extends SocketMessageV2> void checkResent(ChannelHandlerContext ctx, SocketMessageSenderHolderV2<E> senderHolder) {
        while (true) {
            SocketMessageSenderV2<E> sender = senderHolder.getSender();
            if (sender == null) {
                break;
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(sender.getExpiredTime())) {
                sender.trySend(ctx, senderHolder);
            }
        }
    }

    public static void resolve(CtlSerialMessageV2 message, SocketMessageSenderHolderV2<SocketMessageV2> senderHolder) {
        SocketMessageSenderV2<SocketMessageV2> sender = senderHolder.getSender();
        senderHolder.resolve(sender);
        triggerCallback(message, sender);
    }

    private static <E extends SocketMessageV2> void triggerCallback(E message, SocketMessageSenderV2<E> sender) {
        if (sender != null) {
            sender.triggerSuccess(message);
        }
    }
}