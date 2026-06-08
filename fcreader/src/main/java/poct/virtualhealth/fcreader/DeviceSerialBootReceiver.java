package poct.virtualhealth.fcreader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeviceSerialBootReceiver extends BroadcastReceiver {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult result = goAsync();
        EXECUTOR.execute(() -> {
            try {
                DeviceSerialFileTask.run(context);
            } finally {
                result.finish();
            }
        });
    }
}
