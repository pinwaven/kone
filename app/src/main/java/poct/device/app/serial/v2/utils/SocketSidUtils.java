package poct.device.app.serial.v2.utils;

import java.util.concurrent.atomic.AtomicReference;

public class SocketSidUtils {
    private static final AtomicReference<Short> CONTROL_SID = new AtomicReference<>((short) 1);
    private static final AtomicReference<Short> FEEDBACK_SID = new AtomicReference<>((short) 0);

    public SocketSidUtils() {
    }

    public static synchronized short nextControlSid() {
        return nextSid(CONTROL_SID);
    }

    public static synchronized short nextFeedbackSid() {
        return nextSid(FEEDBACK_SID);
    }

    private static synchronized short nextSid(AtomicReference<Short> ref) {
        Short aShort = ref.get();
        aShort = (short) (aShort.intValue() + 2);
        ref.set(aShort);
        return aShort;
    }
}