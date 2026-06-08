package poct.virtualhealth.fcreader;

import android.os.Build;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class AndroidSerialSources {
    private static final String[] SERIAL_PROPERTIES = new String[]{
            "ro.serialno",
            "ro.boot.serialno",
            "vendor.serialno",
            "persist.vendor.serialno",
            "persist.sys.serialno",
            "ril.serialnumber"
    };

    private AndroidSerialSources() {
    }

    public static List<SerialNumberResolver.SerialSource> create() {
        List<SerialNumberResolver.SerialSource> sources = new ArrayList<>();
        sources.add(AndroidSerialSources::readBuildSerial);
        for (String property : SERIAL_PROPERTIES) {
            sources.add(() -> readSystemProperty(property));
        }
        return sources;
    }

    private static String readBuildSerial() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Build.getSerial();
        }
        return Build.SERIAL;
    }

    private static String readSystemProperty(String name) throws Exception {
        Class<?> systemProperties = Class.forName("android.os.SystemProperties");
        Method get = systemProperties.getMethod("get", String.class);
        Object value = get.invoke(null, name);
        return value == null ? null : value.toString();
    }
}
