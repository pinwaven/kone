package poct.virtualhealth.fcreader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class SerialFileWriter {
    private SerialFileWriter() {
    }

    public static void write(File outputFile, String serial) throws IOException {
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Unable to create directory: " + parent.getAbsolutePath());
        }

        String content = (serial == null ? SerialNumberResolver.UNKNOWN_SERIAL : serial.trim()) + "\n";
        try (FileOutputStream stream = new FileOutputStream(outputFile, false)) {
            stream.write(content.getBytes(StandardCharsets.UTF_8));
            stream.flush();
        }
        outputFile.setReadable(true, false);
    }
}
