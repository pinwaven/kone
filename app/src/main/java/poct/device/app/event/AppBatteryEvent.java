package poct.device.app.event;

import timber.log.Timber;

public class AppBatteryEvent {
    private final int percent; // 0-100
    private final boolean plugged; // 0-100

    public AppBatteryEvent(int percent, boolean plugged) {
        Timber.w("AppBatteryEvent percent: %s", percent);
        this.percent = percent;
        this.plugged = plugged;
    }

    public int getPercent() {
        return percent;
    }

    public boolean isPlugged() {
        return plugged;
    }
}
