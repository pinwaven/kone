package poct.device.app.serial.v2.ctl

import timber.log.Timber

/**
 * Retry-with-homing brain for mechanical actuation on the main work flow.
 *
 * A stepper move that failed or timed out may have left the carriage part-way through
 * its travel. Re-issuing the same move blindly can drive it into a hard stop and jam the
 * mechanism, so between attempts we [recover] (home) back to a known position first.
 *
 * Kept free of serial/Android types on purpose — the part that has to be right is the
 * attempt/recover ordering, so it stays synchronously unit-testable.
 */
object MechanicalRetry {

    /** Retries allowed after the first attempt. 1 initial + [MAX_RETRIES] = 4 tries total. */
    const val MAX_RETRIES = 3

    /**
     * Runs a mechanical [operation] until it completes or the retry budget is exhausted.
     *
     * @param name for logs
     * @param recover returns the mechanism to a known (homed) position before a retry;
     *        runs before each retry but never after the final attempt. A failed recovery
     *        is logged but does not abort — the next attempt still runs.
     * @param operation performs send + wait-for-completion; returns true on completion.
     * @return true if any attempt completed, false if all [MAX_RETRIES]+1 attempts failed.
     */
    suspend fun run(
        name: String,
        maxRetries: Int = MAX_RETRIES,
        recover: suspend () -> Unit,
        operation: suspend () -> Boolean,
    ): Boolean {
        for (attempt in 0..maxRetries) {
            if (operation()) {
                if (attempt > 0) Timber.i("$name completed on retry $attempt/$maxRetries")
                return true
            }
            Timber.e("$name failed attempt ${attempt + 1}/${maxRetries + 1}")

            val isLastAttempt = attempt == maxRetries
            if (!isLastAttempt) {
                runCatching { recover() }
                    .onFailure { Timber.e(it, "$name homing recovery failed; retrying anyway") }
            }
        }
        return false
    }
}
