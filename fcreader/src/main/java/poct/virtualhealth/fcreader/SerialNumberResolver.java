package poct.virtualhealth.fcreader;

import java.util.List;

public final class SerialNumberResolver {
    public static final String UNKNOWN_SERIAL = "unknown";

    private SerialNumberResolver() {
    }

    public static String resolve(List<SerialSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return UNKNOWN_SERIAL;
        }

        for (SerialSource source : sources) {
            String serial = readSafely(source);
            if (isKnownSerial(serial)) {
                return serial.trim();
            }
        }
        return UNKNOWN_SERIAL;
    }

    private static String readSafely(SerialSource source) {
        if (source == null) {
            return null;
        }
        try {
            return source.read();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isKnownSerial(String serial) {
        if (serial == null) {
            return false;
        }

        String trimmed = serial.trim();
        return !trimmed.isEmpty()
                && !UNKNOWN_SERIAL.equalsIgnoreCase(trimmed)
                && !"null".equalsIgnoreCase(trimmed);
    }

    public interface SerialSource {
        String read() throws Exception;
    }
}
