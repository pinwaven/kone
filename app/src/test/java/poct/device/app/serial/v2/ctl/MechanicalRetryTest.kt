package poct.device.app.serial.v2.ctl

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Retry-with-homing brain for mechanical actuation — see WorkMainViewModel main flow.
 *
 * Pure attempt loop: no serial/Android types, so the ordering rules that carry weight
 * (attempt count, home-between-retries, stop-on-success) stay synchronously testable.
 */
class MechanicalRetryTest {

    @Test
    fun `succeeds on the first attempt without homing`() = runBlocking {
        val log = Recorder()
        val ok = MechanicalRetry.run(name = "moveOut", recover = log.recover) {
            log.attempt(succeed = true)
        }

        assertTrue(ok)
        assertEquals(1, log.attempts)
        assertEquals("no recovery before a first-try success", 0, log.recoveries)
    }

    @Test
    fun `homes then retries after a failure, succeeding on the second attempt`() = runBlocking {
        val log = Recorder()
        var call = 0
        val ok = MechanicalRetry.run(name = "moveOut", recover = log.recover) {
            log.attempt(succeed = ++call == 2) // fail once, then succeed
        }

        assertTrue(ok)
        assertEquals(2, log.attempts)
        assertEquals("one homing between the two attempts", 1, log.recoveries)
        assertEquals("home must precede the retry", listOf("attempt", "recover", "attempt"), log.order)
    }

    @Test
    fun `gives up after one initial attempt plus three retries`() = runBlocking {
        val log = Recorder()
        val ok = MechanicalRetry.run(name = "moveOut", recover = log.recover) {
            log.attempt(succeed = false) // never completes
        }

        assertFalse(ok)
        assertEquals("1 initial + 3 retries", 4, log.attempts)
        assertEquals("home before each of the 3 retries, none after the last", 3, log.recoveries)
    }

    @Test
    fun `does not home after the final failed attempt`() = runBlocking {
        // Homing after the last try is wasted motion — the card is about to be ejected.
        val log = Recorder()
        MechanicalRetry.run(name = "homing", recover = log.recover) {
            log.attempt(succeed = false)
        }

        assertEquals("attempt", log.order.last())
    }

    private class Recorder {
        var attempts = 0
        var recoveries = 0
        val order = mutableListOf<String>()

        fun attempt(succeed: Boolean): Boolean {
            attempts++
            order += "attempt"
            return succeed
        }

        val recover: suspend () -> Unit = {
            recoveries++
            order += "recover"
        }
    }
}
