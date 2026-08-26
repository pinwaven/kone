package poct.device.app.thirdparty

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class NanoApiTest {
    @Test
    fun buildClientDefaultsToOriginalTimeouts() {
        val client = NanoApi.buildClient()

        assertEquals(5_000, client.connectTimeoutMillis)
        assertEquals(15_000, client.readTimeoutMillis)
        assertEquals(10_000, client.writeTimeoutMillis)
    }

    @Test
    fun buildClientAppliesCustomTimeouts() {
        val client = NanoApi.buildClient(
            connectTimeoutSec = 8,
            readTimeoutSec = 30,
            writeTimeoutSec = 60,
        )

        assertEquals(TimeUnit.SECONDS.toMillis(8).toInt(), client.connectTimeoutMillis)
        assertEquals(TimeUnit.SECONDS.toMillis(30).toInt(), client.readTimeoutMillis)
        assertEquals(TimeUnit.SECONDS.toMillis(60).toInt(), client.writeTimeoutMillis)
    }
}
