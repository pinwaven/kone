package poct.device.app.thirdparty.nano

import org.junit.Assert.assertEquals
import org.junit.Test
import poct.device.app.thirdparty.model.nano.NanoEndpoints

class NanoEndpointsTest {
    @Test
    fun kinoEndpointsUseNewFunctionPaths() {
        val base = "https://nano-test.fros.cc/"

        assertEquals(
            "https://nano-test.fros.cc/kino/kino-chip?chip_id=__ping__",
            NanoEndpoints.probe(base)
        )
        assertEquals(
            "https://nano-test.fros.cc/kino/kino-chip?chip_id=ABC+123",
            NanoEndpoints.kinoChip(base, "ABC 123")
        )
        assertEquals("https://nano-test.fros.cc/kino/biomarkers", NanoEndpoints.biomarkers(base))
        assertEquals("https://nano-test.fros.cc/kino/kino-result", NanoEndpoints.kinoResult(base))
        assertEquals("https://nano-test.fros.cc/kino/kino-machines/info", NanoEndpoints.machineInfo(base))
        assertEquals("https://nano-test.fros.cc/kino/kino-upgrade", NanoEndpoints.kinoUpgrade(base))
        assertEquals("https://nano-test.fros.cc/kino/token/exchange", NanoEndpoints.tokenExchange(base))
    }
}
