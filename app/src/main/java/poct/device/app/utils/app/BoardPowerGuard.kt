package poct.device.app.utils.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import poct.device.app.bean.BoardPowerGuardBean
import poct.device.app.entity.service.SysConfigService
import timber.log.Timber

object BoardPowerGuard {
    const val STABLE_DELAY_MS = 10_000L
    const val RETRY_DEBOUNCE_MS = 2_000L
    const val AGING_BATTERY_THRESHOLD = 40

    private val HI_UID_HEX_REGEX = Regex("^[0-9a-fA-F]{24}$")

    sealed interface Decision {
        object ProceedNormal : Decision
        data class BlockNeedCharger(val agingWarn: Boolean) : Decision
    }

    /**
     * 纯决策逻辑，不触碰数据库，方便单测。
     */
    fun decideFrom(pending: Boolean, batteryAtPending: Int, plugged: Boolean?): Decision {
        if (!pending) return Decision.ProceedNormal
        if (plugged == true) return Decision.ProceedNormal
        // If plugged is unknown (null), don't warn about aging battery
        if (plugged == null) return Decision.BlockNeedCharger(agingWarn = false)
        return Decision.BlockNeedCharger(agingWarn = batteryAtPending > AGING_BATTERY_THRESHOLD)
    }

    suspend fun decide(rawBattery: Int, plugged: Boolean?): Decision {
        val bean = SysConfigService.findBean(BoardPowerGuardBean.PREFIX, BoardPowerGuardBean::class)
        val pending = bean.pending.toBoolean()
        val batteryAtPending = bean.batteryAtPending.toIntOrNull() ?: -1
        return decideFrom(pending, batteryAtPending, plugged)
    }

    /**
     * 每次真正尝试上电前调用，覆盖写入。写入失败返回 false（调用方降级为放行上电）。
     */
    suspend fun markPending(rawBattery: Int): Boolean {
        return try {
            SysConfigService.saveBean(
                BoardPowerGuardBean.PREFIX,
                BoardPowerGuardBean(pending = "true", batteryAtPending = rawBattery.toString())
            )
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "BoardPowerGuard markPending failed")
            false
        }
    }

    /**
     * 握手成功 + 稳定期过后调用，清 pending。
     */
    suspend fun markConfirmedStable() {
        try {
            SysConfigService.saveBean(
                BoardPowerGuardBean.PREFIX,
                BoardPowerGuardBean(pending = "false", batteryAtPending = "-1")
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "BoardPowerGuard markConfirmedStable failed")
        }
    }

    /**
     * 主动重启/关机前同步清 pending，避免进程马上退出导致未写入、
     * 被下次开机误判为崩溃循环。
     */
    fun clearForIntentionalRestartBlocking() {
        try {
            runBlocking(Dispatchers.IO) {
                SysConfigService.saveBean(
                    BoardPowerGuardBean.PREFIX,
                    BoardPowerGuardBean(pending = "false", batteryAtPending = "-1")
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "BoardPowerGuard clearForIntentionalRestartBlocking failed")
        }
    }

    /**
     * 校验主板 HI 握手响应是否符合协议格式：`!|ver:<version>~<24位十六进制UID>`
     * 见 docs/device-hardware-api.md 5.2 节。
     */
    fun isValidHiResult(hiResult: String): Boolean {
        if (!hiResult.contains("ver:")) return false
        val uidPart = hiResult.substringAfter("~", "").trim()
        return HI_UID_HEX_REGEX.matches(uidPart)
    }
}
