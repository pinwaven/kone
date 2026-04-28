package poct.device.app.utils.common;

import org.greenrobot.eventbus.EventBus;

public class EventUtils {
    public EventUtils() {
    }

    public static void publishEvent(Object event) {
        EventBus.getDefault().postSticky(event);
    }
}
