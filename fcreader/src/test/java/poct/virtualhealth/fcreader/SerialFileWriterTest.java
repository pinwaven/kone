package poct.virtualhealth.fcreader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;

public class SerialFileWriterTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void writesTrimmedSerialWithTrailingNewline() throws Exception {
        File outputFile = new File(temporaryFolder.getRoot(), "device_serial.txt");

        SerialFileWriter.write(outputFile, " VH-FC-002 ");

        assertEquals("VH-FC-002\n", read(outputFile));
    }

    @Test
    public void overwritesExistingContent() throws Exception {
        File outputFile = new File(temporaryFolder.getRoot(), "device_serial.txt");
        Files.write(outputFile.toPath(), "old\n".getBytes(StandardCharsets.UTF_8));

        SerialFileWriter.write(outputFile, "new");

        assertEquals("new\n", read(outputFile));
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }
}
