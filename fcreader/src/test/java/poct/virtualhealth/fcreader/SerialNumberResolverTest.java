package poct.virtualhealth.fcreader;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class SerialNumberResolverTest {
    @Test
    public void returnsFirstNonBlankKnownSerial() {
        String serial = SerialNumberResolver.resolve(Arrays.asList(
                () -> "",
                () -> " unknown ",
                () -> " VH-FC-001 "
        ));

        assertEquals("VH-FC-001", serial);
    }

    @Test
    public void fallsBackToUnknownWhenNoSerialExists() {
        String serial = SerialNumberResolver.resolve(Arrays.asList(
                () -> null,
                () -> "   ",
                () -> "UNKNOWN"
        ));

        assertEquals(SerialNumberResolver.UNKNOWN_SERIAL, serial);
    }

    @Test
    public void ignoresFailingSerialSources() {
        String serial = SerialNumberResolver.resolve(Arrays.asList(
                () -> {
                    throw new IllegalStateException("blocked");
                },
                () -> "ro-serial-123"
        ));

        assertEquals("ro-serial-123", serial);
    }

    @Test
    public void handlesEmptySources() {
        assertEquals(
                SerialNumberResolver.UNKNOWN_SERIAL,
                SerialNumberResolver.resolve(Collections.emptyList())
        );
    }
}
