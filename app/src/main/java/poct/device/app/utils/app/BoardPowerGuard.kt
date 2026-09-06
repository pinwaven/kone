package poct.device.app.utils.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import poct.device.app.App
import poct.device.app.bean.BoardPowerGuardBean
import poct.device.app.entity.service.SysConfigService
import timber.log.Timber

object BoardPowerGuard {
    const val STABLE_DELAY_MS = 10_000L
    const val RETRY_DEBOUNCE_MS = 2_000L
    const val AGING_BATTERY_THRESHOLD = 40

    /**
     * 未插电时，电量比上次崩溃记录涨多少才当作"确实在充电/恢复"放行重试。
     * 1%量级的涨幅在老化电池卸载后本身就有电压回弹噪声，不能当真在充电，
     * 门槛拉到5%降低误判概率（仍然是启发式，不是精确判断）。
     */
    private const val BATTERY_RECOVERY_THRESHOLD = 5

    private const val PREFS_NAME = "board_power_guard"
    private const val PREF_PENDING = "pending"
    private const val PREF_BATTERY_AT_PENDING = "battery_at_pending"
    private val HI_UID_HEX_REGEX = Regex("^[0-9a-fA-F]{24}$")

    sealed interface Decision {
        object ProceedNormal : Decision
        data class BlockNeedCharger(val agingWarn: Boolean) : Decision
    }

    /**
     * 纯决策逻辑，不触碰数据库，方便单测。
     */
    fun decideFrom(
        pending: Boolean,
        batteryAtPending: Int,
        currentBattery: Int,
        plugged: Boolean?,
    ): Decision {
        if (!pending) return Decision.ProceedNormal
        if (plugged == true) return Decision.ProceedNormal
        // If plugged is unknown (null), don't warn about aging battery
        if (plugged == null) return Decision.BlockNeedCharger(agingWarn = false)
        if (currentBattery - batteryAtPending >= BATTERY_RECOVERY_THRESHOLD) return Decision.ProceedNormal
        return Decision.BlockNeedCharger(agingWarn = batteryAtPending > AGING_BATTERY_THRESHOLD)
    }

    suspend fun decide(rawBattery: Int, plugged: Boolean?): Decision {
        val bean = SysConfigService.findBean(BoardPowerGuardBean.PREFIX, BoardPowerGuardBean::class)
        val dbPending = bean.pending.toBoolean()
        val fastPending = readFastPending()
        val pending = dbPending || fastPending.pending
        val batteryAtPending = if (dbPending) {
            bean.batteryAtPending.toIntOrNull() ?: -1
        } else if (fastPending.pending) {
            fastPending.batteryAtPending
        } else {
            rawBattery
        }
        if (fastPending.pending && !dbPending) {
            Timber.w(
                "BoardPowerGuard recovered pending from fast mirror: batteryAtPending=%s",
                fastPending.batteryAtPending
            )
            // 补写 Room 只是让下次读取不必再依赖快速镜像；这里失败不能影响本次已经
            // 算出来的 decision，否则一次补写异常就会把已经识别出的"该拦"错误地
            // 变成放行（decide() 抛出会被调用方 fail-open 成 ProceedNormal）
            try {
                saveDbPending(fastPending.batteryAtPending)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "BoardPowerGuard recover-to-db write failed, keep fast-mirror decision")
            }
        }
        return decideFrom(pending, batteryAtPending, rawBattery, plugged)
    }

    /**
     * 每次真正尝试上电前调用，覆盖写入。
     *
     * 先用 SharedPreferences commit() 写入轻量级快速镜像，再写 Room/SQLite。
     * 实机低电瞬断时，快速镜像比 Room 初始化和多行写入更早、更轻，下一次
     * 启动可通过它恢复 pending 并阻断继续上电。
     */
    suspend fun markPending(rawBattery: Int): Boolean {
        val fastMarked = markFastPending(rawBattery)
        if (!fastMarked) {
            Timber.e("BoardPowerGuard fast pending mirror write failed")
        }
        return try {
            saveDbPending(rawBattery)
            Timber.w("BoardPowerGuard pending marked: battery=%s fastMirror=%s", rawBattery, fastMarked)
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "BoardPowerGuard markPending failed")
            fastMarked
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
            clearFastPending()
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
                clearFastPending()
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

    private suspend fun saveDbPending(rawBattery: Int) {
        SysConfigService.saveBean(
            BoardPowerGuardBean.PREFIX,
            BoardPowerGuardBean(pending = "true", batteryAtPending = rawBattery.toString())
        )
    }

    private fun markFastPending(rawBattery: Int): Boolean {
        return prefs()
            .edit()
            .putBoolean(PREF_PENDING, true)
            .putInt(PREF_BATTERY_AT_PENDING, rawBattery)
            .commit()
    }

    private fun clearFastPending(): Boolean {
        return prefs()
            .edit()
            .putBoolean(PREF_PENDING, false)
            .putInt(PREF_BATTERY_AT_PENDING, -1)
            .commit()
    }

    private fun readFastPending(): FastPending {
        val prefs = prefs()
        return FastPending(
            pending = prefs.getBoolean(PREF_PENDING, false),
            batteryAtPending = prefs.getInt(PREF_BATTERY_AT_PENDING, -1)
        )
    }

    private fun prefs() = App.getContext()
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private data class FastPending(
        val pending: Boolean,
        val batteryAtPending: Int,
    )
}
