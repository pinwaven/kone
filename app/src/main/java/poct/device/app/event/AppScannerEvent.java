package poct.device.app.event;

import android.content.Context;
import android.content.Intent;

public class AppScannerEvent {
    private final Context context;
    private final Intent intent;

    public AppScannerEvent(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }


    public Context getContext() {
        return context;
    }

    public Intent getIntent() {
        return intent;
    }
}
