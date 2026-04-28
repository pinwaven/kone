package poct.device.app.utils.app;

import org.greenrobot.eventbus.EventBus;

public class AppEventUtils {
    public static void publishEvent(Object event) {
        EventBus.getDefault().postSticky(event);
    }

    public static void register(Object subscriber) {
        if (!EventBus.getDefault().isRegistered(subscriber)) {
            EventBus.getDefault().register(subscriber);
        }
    }

    public static void unregister(Object subscriber) {
        if (EventBus.getDefault().isRegistered(subscriber)) {
            EventBus.getDefault().unregister(subscriber);
        }
    }
}