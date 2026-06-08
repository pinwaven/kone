package poct.virtualhealth.fcreader;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public final class DeviceSerialFileTask {
    public static final String FILE_NAME = "device_serial.txt";
    public static final String PROVIDER_AUTHORITY = "poct.virtualhealth.fcreader.serial";
    public static final String PROVIDER_URI = "content://" + PROVIDER_AUTHORITY + "/" + FILE_NAME;
    private static final String TAG = "FcReader";

    private DeviceSerialFileTask() {
    }

    public static boolean run(Context context) {
        String serial = SerialNumberResolver.resolve(AndroidSerialSources.create());
        File outputFile = getOutputFile(context);
        try {
            SerialFileWriter.write(outputFile, serial);
            Log.i(TAG, "Device serial written to " + outputFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to write device serial to " + outputFile.getAbsolutePath(), e);
            return false;
        }
    }

    public static File getOutputFile(Context context) {
        Context storageContext = context.createDeviceProtectedStorageContext();
        return new File(storageContext.getFilesDir(), FILE_NAME);
    }
}
