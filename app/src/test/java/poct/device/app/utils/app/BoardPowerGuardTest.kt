package poct.device.app.utils.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardPowerGuardTest {
    @Test
    fun decideFromReturnsProceedNormalWhenNoPending() {
        val decision = BoardPowerGuard.decideFrom(
            pending = false,
            batteryAtPending = 55,
            currentBattery = 55,
            plugged = false
        )
        assertEquals(BoardPowerGuard.Decision.ProceedNormal, decision)
    }

    @Test
    fun decideFromReturnsProceedNormalWhenPendingAndPlugged() {
        val decision = BoardPowerGuard.decideFrom(
            pending = true,
            batteryAtPending = 55,
            currentBattery = 55,
            plugged = true
        )
        assertEquals(BoardPowerGuard.Decision.ProceedNormal, decision)
    }

    @Test
    fun decideFromBlocksWithoutAgingWarnWhenBatteryAtOrBelowThreshold() {
        val decision = BoardPowerGuard.decideFrom(
            pending = true,
            batteryAtPending = 40,
            currentBattery = 40,
            plugged = false
        )
        assertEquals(BoardPowerGuard.Decision.BlockNeedCharger(agingWarn = false), decision)
    }

    @Test
    fun decideFromBlocksWithAgingWarnWhenBatteryAboveThreshold() {
        val decision = BoardPowerGuard.decideFrom(
            pending = true,
            batteryAtPending = 41,
            currentBattery = 41,
            plugged = false
        )
        assertEquals(BoardPowerGuard.Decision.BlockNeedCharger(agingWarn = true), decision)
    }

    @Test
    fun decideFromBlocksWithoutAgingWarnWhenPluggedStateUnknown() {
        val decision = BoardPowerGuard.decideFrom(
            pending = true,
            batteryAtPending = 90,
            currentBattery = 95,
            plugged = null
        )
        assertEquals(BoardPowerGuard.Decision.BlockNeedCharger(agingWarn = false), decision)
    }

    @Test
    fun decideFromStillBlocksWhenUnpluggedBatteryRoseByOnlyOnePercent() {
        // 1% 涨幅在老化电池卸载后本身就有电压回弹噪声，不算真的在充电
        val decision = BoardPowerGuard.decideFrom(
            pending = true,
            batteryAtPending = 12,
            currentBattery = 13,
            plugged = false
        )
        assertEquals(BoardPowerGuard.Decision.BlockNeedCharger(agingWarn = false), decision)
    }

    @Test
    fun decideFromStillBlocksWhenUnpluggedBatteryDidNotIncreaseAfterPending() {
        val decision = BoardPowerGuard.decideFrom(
            pending = true,
            batteryAtPending = 12,
            currentBattery = 12,
            plugged = false
        )
        assertEquals(BoardPowerGuard.Decision.BlockNeedCharger(agingWarn = false), decision)
    }

    @Test
    fun decideFromReturnsProceedNormalWhenUnpluggedBatteryRoseByThreshold() {
        val decision = BoardPowerGuard.decideFrom(
            pending = true,
            batteryAtPending = 12,
            currentBattery = 17,
            plugged = false
        )
        assertEquals(BoardPowerGuard.Decision.ProceedNormal, decision)
    }

    @Test
    fun decideFromStillBlocksWhenUnpluggedBatteryRoseJustBelowThreshold() {
        val decision = BoardPowerGuard.decideFrom(
            pending = true,
            batteryAtPending = 12,
            currentBattery = 16,
            plugged = false
        )
        assertEquals(BoardPowerGuard.Decision.BlockNeedCharger(agingWarn = false), decision)
    }

    @Test
    fun isValidHiResultRejectsBlankResponse() {
        assertFalse(BoardPowerGuard.isValidHiResult(""))
    }

    @Test
    fun isValidHiResultAcceptsWellFormedHandshake() {
        assertTrue(BoardPowerGuard.isValidHiResult("!|ver:v0.1.7~aabbccddeeff112233445566"))
    }

    @Test
    fun isValidHiResultRejectsResponseMissingVerMarker() {
        assertFalse(BoardPowerGuard.isValidHiResult("garbage"))
    }

    @Test
    fun isValidHiResultRejectsWrongLengthUid() {
        assertFalse(BoardPowerGuard.isValidHiResult("!|ver:v0.1.7~abc"))
    }
}
