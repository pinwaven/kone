# 电池老化保护：主板上电重启循环防护 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent devices with aging batteries from getting stuck in an "boot → brownout-reboot → boot" loop by tracking a persisted "board power attempt pending" flag, skipping the risky board power-on when that flag survived an unclean restart and no charger is connected, and gating the main flow until the board is confirmed powered.

**Architecture:** A new `BoardPowerGuard` util (backed by the existing `SysConfig` key-value table) decides whether to proceed or block before every board power-on attempt. A single new entry point `App.attemptBoardPowerOn()` replaces all scattered `powerOnCtlBoard()`/`openSerialPort()` call sites (app start, screen-on recovery). `AppParams` exposes `boardPowerBlocked`/`boardPowerAgingWarn` as `StateFlow`s that a new Compose overlay observes to lock (or banner-warn) the main nav graph.

**Tech Stack:** Kotlin, Jetpack Compose, Room (via existing `SysConfig` entity/DAO/service), Kotlin Coroutines (`Mutex`, `StateFlow`), JUnit4 (existing test setup, no new test deps).

**Spec:** [docs/superpowers/specs/2026-09-04-board-power-guard-design.md](../specs/2026-09-04-board-power-guard-design.md) — read both together; this plan assumes the spec's decisions (10s stability delay, 2s retry debounce, 40% aging threshold, exact dialog copy) without repeating the rationale.

## Global Constraints

- Stability delay before a board power-on attempt is considered confirmed: **10 seconds** (`BoardPowerGuard.STABLE_DELAY_MS = 10_000L`).
- Retry debounce for repeated power-on attempts: **2 seconds** (`BoardPowerGuard.RETRY_DEBOUNCE_MS = 2_000L`).
- Aging-warning battery threshold: **> 40%** raw battery at the moment the pending flag was written.
- Full-screen lock copy: `"电池电量低，请充电"` (string resource `msg_board_power_need_charger`).
- Aging warning copy: `"电池老化，请联系客服更换电池"` (string resource `msg_board_power_aging_warn`), shown once per lock cycle only.
- Exempt route groups (banner instead of full-screen lock): `RouteConfig.SETTING`, `RouteConfig.AFTER_SALE`, `RouteConfig.SYS_FUN`.
- `ui/sample/SampleSerial.kt`'s manual `powerOn()`/`powerOff()` are an explicit known exception — **do not** route them through `attemptBoardPowerOn()` in this plan (see spec §10).
- `SysConfigService.findBean`/`saveBean` only supports `String`-typed `ConfigBean` properties (reflection cast to `KMutableProperty1<T, String>`) — any new config bean field must be `String`.

---

### Task 1: `BoardPowerGuardBean` + string resources

**Files:**
- Create: `app/src/main/java/poct/device/app/bean/BoardPowerGuardBean.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Interfaces:**
- Produces: `BoardPowerGuardBean(pending: String = "false", batteryAtPending: String = "-1") : ConfigBean` with `companion object { const val PREFIX = "board_power_guard_" }`
- Produces: string resources `R.string.msg_board_power_need_charger`, `R.string.msg_board_power_aging_warn`

- [ ] **Step 1: Create the config bean**

```kotlin
package poct.device.app.bean

/**
 * 主板欠压重启保护状态。pending/batteryAtPending 均以字符串存储，
 * 因 SysConfigService 反射映射仅支持 ConfigBean 的 String 属性。
 */
data class BoardPowerGuardBean(
    var pending: String = "false",
    var batteryAtPending: String = "-1",
) : ConfigBean {
    companion object {
        const val PREFIX = "board_power_guard_"
    }
}
```

- [ ] **Step 2: Add string resources**

In `app/src/main/res/values/strings.xml`, add near the existing `msg_restart_confirm`/`msg_shutdown_confirm` block (around line 26-28):

```xml
    <string name="msg_board_power_need_charger">电池电量低，请充电</string>
    <string name="msg_board_power_aging_warn">电池老化，请联系客服更换电池</string>
```

In `app/src/main/res/values-en/strings.xml`, add at the corresponding location:

```xml
    <string name="msg_board_power_need_charger">Battery low, please charge the device</string>
    <string name="msg_board_power_aging_warn">Battery aging detected, please contact customer service to replace it</string>
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/poct/device/app/bean/BoardPowerGuardBean.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml
git commit -m "feat: add BoardPowerGuardBean config bean and lock-screen strings"
```

---

### Task 2: `BoardPowerGuard` util + unit tests

**Files:**
- Create: `app/src/main/java/poct/device/app/utils/app/BoardPowerGuard.kt`
- Test: `app/src/test/java/poct/device/app/utils/app/BoardPowerGuardTest.kt`

**Interfaces:**
- Consumes: `BoardPowerGuardBean` (Task 1), `poct.device.app.entity.service.SysConfigService.findBean/saveBean` (existing, `suspend fun <T: ConfigBean> findBean(prefix: String, klass: KClass<T>): T` / `suspend fun <T: ConfigBean> saveBean(prefix: String, bean: T)`)
- Produces:
  - `BoardPowerGuard.Decision` sealed interface: `ProceedNormal`, `BlockNeedCharger(agingWarn: Boolean)`
  - `BoardPowerGuard.decideFrom(pending: Boolean, batteryAtPending: Int, plugged: Boolean?): Decision` — pure, used by later tasks and by tests
  - `suspend fun decide(rawBattery: Int, plugged: Boolean?): Decision`
  - `suspend fun markPending(rawBattery: Int): Boolean`
  - `suspend fun markConfirmedStable()`
  - `fun clearForIntentionalRestartBlocking()`
  - `fun isValidHiResult(hiResult: String): Boolean`
  - `const val STABLE_DELAY_MS: Long`, `const val RETRY_DEBOUNCE_MS: Long`, `const val AGING_BATTERY_THRESHOLD: Int`

Note on `isValidHiResult`: the design spec left the exact validity check as an open question (§10). Per `docs/device-hardware-api.md:174-183`, the real HI response wire format is `!|ver:v0.1.7~<UID_W0><UID_W1><UID_W2>` — three 8-hex-char words, i.e. exactly 24 hex characters after `~`. This plan validates against that documented format directly (regex match on the UID segment) rather than reusing `NanoAuthSupport.extractFirmwareVersion/Id`, because those Nano-specific helpers are lenient fallback parsers that return non-blank output even for garbage input (e.g. `"garbage"` → both return `"garbage"`), so they don't actually reject malformed handshakes.

- [ ] **Step 1: Write the failing tests**

```kotlin
package poct.device.app.utils.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardPowerGuardTest {
    @Test
    fun decideFromReturnsProceedNormalWhenNoPending() {
        val decision = BoardPowerGuard.decideFrom(pending = false, batteryAtPending = 55, plugged = false)
        assertEquals(BoardPowerGuard.Decision.ProceedNormal, decision)
    }

    @Test
    fun decideFromReturnsProceedNormalWhenPendingAndPlugged() {
        val decision = BoardPowerGuard.decideFrom(pending = true, batteryAtPending = 55, plugged = true)
        assertEquals(BoardPowerGuard.Decision.ProceedNormal, decision)
    }

    @Test
    fun decideFromBlocksWithoutAgingWarnWhenBatteryAtOrBelowThreshold() {
        val decision = BoardPowerGuard.decideFrom(pending = true, batteryAtPending = 40, plugged = false)
        assertEquals(BoardPowerGuard.Decision.BlockNeedCharger(agingWarn = false), decision)
    }

    @Test
    fun decideFromBlocksWithAgingWarnWhenBatteryAboveThreshold() {
        val decision = BoardPowerGuard.decideFrom(pending = true, batteryAtPending = 41, plugged = false)
        assertEquals(BoardPowerGuard.Decision.BlockNeedCharger(agingWarn = true), decision)
    }

    @Test
    fun decideFromBlocksWithoutAgingWarnWhenPluggedStateUnknown() {
        val decision = BoardPowerGuard.decideFrom(pending = true, batteryAtPending = 90, plugged = null)
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
```

- [ ] **Step 2: Run tests to verify they fail (compile error — `BoardPowerGuard` doesn't exist yet)**

Run: `./gradlew test --tests "poct.device.app.utils.app.BoardPowerGuardTest"`
Expected: FAIL (compilation error, `BoardPowerGuard` unresolved reference)

- [ ] **Step 3: Implement `BoardPowerGuard`**

```kotlin
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
        data object ProceedNormal : Decision
        data class BlockNeedCharger(val agingWarn: Boolean) : Decision
    }

    /**
     * 纯决策逻辑，不触碰数据库，方便单测。
     */
    fun decideFrom(pending: Boolean, batteryAtPending: Int, plugged: Boolean?): Decision {
        if (!pending) return Decision.ProceedNormal
        if (plugged == true) return Decision.ProceedNormal
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "poct.device.app.utils.app.BoardPowerGuardTest"`
Expected: PASS (9 tests)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/poct/device/app/utils/app/BoardPowerGuard.kt app/src/test/java/poct/device/app/utils/app/BoardPowerGuardTest.kt
git commit -m "feat: add BoardPowerGuard decision logic with unit tests"
```

---

### Task 3: `AppParams` lock state

**Files:**
- Modify: `app/src/main/java/poct/device/app/AppParams.kt:87`

**Interfaces:**
- Produces: `AppParams.boardPowerBlocked: StateFlow<Boolean>`, `AppParams.boardPowerAgingWarn: StateFlow<Boolean>`, `AppParams.setBoardPowerBlocked(blocked: Boolean, agingWarn: Boolean)`

- [ ] **Step 1: Add the fields**

Current `AppParams.kt:87`:
```kotlin
    var resumeStatus = false
```

Replace with:
```kotlin
    var resumeStatus = false

    /**
     * 主板欠压重启保护：主流程因未确认主板可用而锁定
     */
    val boardPowerBlocked = MutableStateFlow(false)
    val boardPowerAgingWarn = MutableStateFlow(false)

    fun setBoardPowerBlocked(blocked: Boolean, agingWarn: Boolean) {
        boardPowerBlocked.value = blocked
        boardPowerAgingWarn.value = blocked && agingWarn
    }
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/poct/device/app/AppParams.kt
git commit -m "feat: expose board power lock state on AppParams"
```

---

### Task 4: Raw battery snapshot + charger-plugged retry trigger

**Files:**
- Modify: `app/src/main/java/poct/device/app/utils/app/AppBatteryReceiverHelper.kt`

**Interfaces:**
- Consumes: `AppParams.boardPowerBlocked` (Task 3), `App.getContext().attemptBoardPowerOn(reason)` and `BoardPowerAttemptReason` (Task 5 — this task references them but they don't exist until Task 5 compiles; see note below)
- Produces: `data class BatterySnapshot(val percent: Int, val plugged: Boolean?)`, `AppBatteryReceiverHelper.readRawBatteryOnce(context: Context): BatterySnapshot`

Note: this task's edit to `batteryStateReceiver` references `App.getContext().attemptBoardPowerOn(...)` and `BoardPowerAttemptReason`, which are defined in Task 5. Do Task 5 immediately after this one — the project will not compile between these two tasks. If executing via subagent-driven-development, treat Tasks 4 and 5 as one review unit.

- [ ] **Step 1: Add `BatterySnapshot` and `readRawBatteryOnce`**

Current top of `AppBatteryReceiverHelper.kt`:
```kotlin
package poct.device.app.utils.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import poct.device.app.AppParams
import poct.device.app.event.AppBatteryEvent
import poct.device.app.utils.common.EventUtils
import timber.log.Timber

class AppBatteryReceiverHelper {
    companion object {
        // 在应用启动时调用一次，立即同步电量
        fun initBatteryOnAppStart(context: Context) {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            batteryStatus?.let { intent ->
                val current = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val total = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (current >= 0 && total > 0) {
                    val percent = current * 100 / total
                    // 强制立即更新，不使用滤波
                    AppBatteryUtils.forceUpdateDisplayPercent(percent.toFloat())
                    Timber.w(
                        "应用启动时初始化电量: 实际=%s, 显示=%s",
                        percent,
                        AppBatteryUtils.getCurrentDisplayPercent()
                    )
                }
            }
        }
    }
```

Replace with (adds `BatterySnapshot` data class above the class, and `readRawBatteryOnce` inside the companion object):
```kotlin
package poct.device.app.utils.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.BoardPowerAttemptReason
import poct.device.app.event.AppBatteryEvent
import poct.device.app.utils.common.EventUtils
import timber.log.Timber

/**
 * 原始电量快照，不经过 AppBatteryUtils 的平滑/锁定，供 BoardPowerGuard 使用。
 * plugged 为 null 表示无法确认充电状态（ACTION_BATTERY_CHANGED 未携带 EXTRA_PLUGGED）。
 */
data class BatterySnapshot(val percent: Int, val plugged: Boolean?)

class AppBatteryReceiverHelper {
    companion object {
        // 在应用启动时调用一次，立即同步电量
        fun initBatteryOnAppStart(context: Context) {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            batteryStatus?.let { intent ->
                val current = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val total = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (current >= 0 && total > 0) {
                    val percent = current * 100 / total
                    // 强制立即更新，不使用滤波
                    AppBatteryUtils.forceUpdateDisplayPercent(percent.toFloat())
                    Timber.w(
                        "应用启动时初始化电量: 实际=%s, 显示=%s",
                        percent,
                        AppBatteryUtils.getCurrentDisplayPercent()
                    )
                }
            }
        }

        /**
         * 同步读取一次原始电量与充电状态，用于 BoardPowerGuard 的电量快照与阻断判断。
         */
        fun readRawBatteryOnce(context: Context): BatterySnapshot {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            val current = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val total = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val percent = if (current >= 0 && total > 0) current * 100 / total else -1

            val plugged = if (batteryStatus == null || !batteryStatus.hasExtra(BatteryManager.EXTRA_PLUGGED)) {
                null
            } else {
                batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            }

            return BatterySnapshot(percent, plugged)
        }
    }
```

- [ ] **Step 2: Track the plugged edge and trigger retry on `batteryStateReceiver`**

Current `batteryStateReceiver` (end of the file):
```kotlin
    val batteryStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val current = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val total = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0

            if (current < 0 || total <= 0) return

            val percent = current * 100 / total
//            Timber.w("电池广播: 实际电量=%s%, 充电状态=%s", percent, plugged)

            // 更新充电状态
            AppBatteryUtils.setChargingState(plugged)

            // 获取显示电量
            val displayPercent = AppBatteryUtils.updateAndGetDisplayPercent(percent.toFloat())
            val displayPercentInt = displayPercent.toInt()

            // 存储到全局参数
            AppParams.battery = displayPercentInt
            AppParams.batteryPlugged = plugged

//            Timber.w("处理后电量: 显示电量=%s", displayPercentInt)

            // 发送事件
            EventUtils.publishEvent(AppBatteryEvent(displayPercentInt, plugged))
        }
    }
}
```

Replace with:
```kotlin
    private var lastPluggedState: Boolean? = null

    val batteryStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val current = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val total = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0

            if (current < 0 || total <= 0) return

            val percent = current * 100 / total
//            Timber.w("电池广播: 实际电量=%s%, 充电状态=%s", percent, plugged)

            // 更新充电状态
            AppBatteryUtils.setChargingState(plugged)

            // 获取显示电量
            val displayPercent = AppBatteryUtils.updateAndGetDisplayPercent(percent.toFloat())
            val displayPercentInt = displayPercent.toInt()

            // 存储到全局参数
            AppParams.battery = displayPercentInt
            AppParams.batteryPlugged = plugged

//            Timber.w("处理后电量: 显示电量=%s", displayPercentInt)

            // 发送事件
            EventUtils.publishEvent(AppBatteryEvent(displayPercentInt, plugged))

            // 充电器插入边沿触发：仅在主流程因 board power guard 被锁定时重试上电
            val wasPlugged = lastPluggedState
            lastPluggedState = plugged
            if (wasPlugged == false && plugged && AppParams.boardPowerBlocked.value) {
                Timber.w("charger plugged while board power blocked, retry board power on")
                App.getContext().attemptBoardPowerOn(BoardPowerAttemptReason.CHARGER_PLUGGED)
            }
        }
    }
}
```

- [ ] **Step 3: Do not compile-check yet — proceed directly to Task 5** (this file references `BoardPowerAttemptReason`/`attemptBoardPowerOn` which Task 5 creates)

---

### Task 5: `App.attemptBoardPowerOn()` — the single board power-on entry point

**Files:**
- Modify: `app/src/main/java/poct/device/app/App.kt`

**Interfaces:**
- Consumes: `BoardPowerGuard.decide/markPending/markConfirmedStable/isValidHiResult` (Task 2), `AppParams.setBoardPowerBlocked` (Task 3), `AppBatteryReceiverHelper.readRawBatteryOnce`/`BatterySnapshot` (Task 4)
- Produces: `enum class BoardPowerAttemptReason { APP_START, CHARGER_PLUGGED, SCREEN_ON_RECOVERY }` (top-level, package `poct.device.app`), `App.attemptBoardPowerOn(reason: BoardPowerAttemptReason)` (public instance method, called as `App.getContext().attemptBoardPowerOn(reason)`)

- [ ] **Step 1: Replace imports and add `BoardPowerAttemptReason`**

Current top of `App.kt`:
```kotlin
package poct.device.app

import android.app.Application
import android.hardware.usb.UsbManager
import android.net.wifi.WifiManager
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.bean.ConfigSysBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.serial.v2.SerialHelperV2
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.thirdparty.NanoApi
import poct.device.app.thirdparty.NanoAuthStore
import poct.device.app.thirdparty.model.nano.NanoAuthSupport
import poct.device.app.utils.app.AppLangUtils
import poct.device.app.utils.app.AppSystemUtils
import poct.device.app.utils.app.DeviceIdUtils
import timber.log.Timber

/**
 * *
 *
 *
 * @desc：application
 */
class App : Application() {
```

Replace with:
```kotlin
package poct.device.app

import android.app.Application
import android.hardware.usb.UsbManager
import android.net.wifi.WifiManager
import android.os.SystemClock
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.bean.ConfigSysBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.serial.v2.SerialHelperV2
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.thirdparty.NanoApi
import poct.device.app.thirdparty.NanoAuthStore
import poct.device.app.thirdparty.model.nano.NanoAuthSupport
import poct.device.app.utils.app.AppBatteryReceiverHelper
import poct.device.app.utils.app.AppLangUtils
import poct.device.app.utils.app.AppSystemUtils
import poct.device.app.utils.app.BoardPowerGuard
import poct.device.app.utils.app.DeviceIdUtils
import timber.log.Timber

/**
 * 主板上电尝试的触发来源，用于日志区分。
 */
enum class BoardPowerAttemptReason {
    APP_START,
    CHARGER_PLUGGED,
    SCREEN_ON_RECOVERY,
}

/**
 * *
 *
 *
 * @desc：application
 */
class App : Application() {
```

- [ ] **Step 2: Replace `onCreate`, `openCtlBoard`, `startupService`, `reportVersionsSilently`**

Current (`App.kt:77-194`, everything from `onCreate` through the end of `reportVersionsSilently`):
```kotlin
    override fun onCreate() {
        super.onCreate()

        context = this
        // 多语言
        AppLangUtils.init(this)
        // 日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }

        PDFBoxResourceLoader.init(this)
        openCtlBoard()
        startupService()
    }

    /**
     * 打开控制板电源
     */
    private fun openCtlBoard() {
        Thread {
            CoroutineScope(Dispatchers.IO).launch {
                AppSystemUtils.powerOffCtlBoard()
                delay(500)
                AppSystemUtils.powerOnCtlBoard()
            }
        }.start()
    }

    /**
     * 启动相关服务
     */
    private fun startupService() {
        Thread {
            CoroutineScope(Dispatchers.IO).launch {
                // TODO 简化信息
                // 串口服务
//                ctlService = CtlSerialServiceV2()
//                ctlService!!.start()

                // 等待供电完成
                delay(2000)
                openSerialPort()
                // power 板上电后第一次请求可能会CRC报错，先poll一次
                CtlCommandsV2.readAllData(CtlCommandsV2.poll())

                // 启动时静默上报软件版本与固件版本
                reportVersionsSilently()

                // 物联网连接
                val configInfo =
                    SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoBean::class)
                Timber.d(configInfo.code)
                // TODO 物联网先不调试
                //            Timber.d("日志测试")
                //            val prefix =
                //                Environment.getExternalStorageDirectory().absolutePath + File.separator + "Android" + File.separator + "Nanovate_AI"
                //            CommService.instance().startIot("192.168.1.16", 7300, 9100, "TM-YG01-00046", prefix)
            }
        }.start()
    }

    /**
     * 启动时静默上报软件版本与固件版本。
     *
     * 尽力而为：无 UI 提示，失败仅记录日志。仅在 Nano 流程下执行
     * （此时才配置了上报接口）。
     *
     * 串口读取固件版本为本地快速操作，同步完成以保证串口指令有序；
     * 网络上报可能受网速影响，放到独立协程后台执行，避免拖慢启动流程。
     */
    private suspend fun reportVersionsSilently() {
        try {
            val sysConfig = SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
            if (!ConfigSysBean.isNanoFlow(sysConfig.flow)) {
                Timber.d("skip silent version report: not nano flow")
                return
            }

            val hiResult = CtlCommandsV2.readAllData(CtlCommandsV2.hi())
            val firmwareVersion = NanoAuthSupport.extractFirmwareVersion(hiResult)
            val firmwareId = NanoAuthSupport.extractFirmwareId(hiResult)

            // 刷新本地缓存的固件版本
            if (firmwareVersion.isNotBlank()) {
                val configBean =
                    SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoV2Bean::class)
                SysConfigService.saveBean(
                    ConfigInfoBean.PREFIX,
                    configBean.copy(hardware = firmwareVersion)
                )
            }
            // 板上电后刷新本地缓存的 firmware_id，供后续 invalid_comm_token 自动
            // 重新 /activate 时使用，不用等人工去工厂测试页手点激活
            NanoAuthStore.updateFirmwareId(firmwareId)

            // 网络上报后台执行，不阻塞启动
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = NanoApi.uploadLocalMachineInfo(firmwareVersion = firmwareVersion)
                    Timber.d(
                        "silent version report: software=%s firmware=%s ok=%s skipped=%s msg=%s",
                        BuildConfig.VERSION_NAME,
                        firmwareVersion,
                        result.ok,
                        result.skipped,
                        result.message
                    )
                } catch (e: Exception) {
                    Timber.w(e, "silent version report upload failed")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "silent version report failed")
        }
    }
```

Replace with:
```kotlin
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val boardPowerAttemptMutex = Mutex()
    private var lastBoardPowerAttemptAt = 0L

    override fun onCreate() {
        super.onCreate()

        context = this
        // 多语言
        AppLangUtils.init(this)
        // 日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }

        PDFBoxResourceLoader.init(this)

        appScope.launch {
            val snapshot = AppBatteryReceiverHelper.readRawBatteryOnce(this@App)
            val decision = BoardPowerGuard.decide(snapshot.percent, snapshot.plugged)
            when (decision) {
                is BoardPowerGuard.Decision.BlockNeedCharger -> {
                    Timber.w(
                        "board power guard: blocking board power-on at startup, agingWarn=%s",
                        decision.agingWarn
                    )
                    AppParams.setBoardPowerBlocked(blocked = true, agingWarn = decision.agingWarn)
                }
                BoardPowerGuard.Decision.ProceedNormal -> {
                    attemptBoardPowerOn(BoardPowerAttemptReason.APP_START)
                }
            }

            // 物联网连接（不依赖主板，独立于 guard 决策执行）
            val configInfo =
                SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoBean::class)
            Timber.d(configInfo.code)
            // TODO 物联网先不调试
            //            Timber.d("日志测试")
            //            val prefix =
            //                Environment.getExternalStorageDirectory().absolutePath + File.separator + "Android" + File.separator + "Nanovate_AI"
            //            CommService.instance().startIot("192.168.1.16", 7300, 9100, "TM-YG01-00046", prefix)
        }
    }

    /**
     * 给主板上电、打开串口、握手确认的唯一入口。所有触发场景（开机、充电器插入重试、
     * 屏幕唤醒恢复）都必须走这里，不能再散落直接调用 powerOnCtlBoard()/openSerialPort()。
     *
     * 用 mutex + 2 秒防抖避免插拔充电器抖动导致短时间内重复上电尝试。
     */
    fun attemptBoardPowerOn(reason: BoardPowerAttemptReason) {
        appScope.launch {
            boardPowerAttemptMutex.withLock {
                val now = SystemClock.elapsedRealtime()
                if (now - lastBoardPowerAttemptAt < BoardPowerGuard.RETRY_DEBOUNCE_MS) {
                    Timber.w("skip board power attempt: debounced, reason=%s", reason)
                    return@withLock
                }
                lastBoardPowerAttemptAt = now

                val snapshot = AppBatteryReceiverHelper.readRawBatteryOnce(this@App)
                if (!BoardPowerGuard.markPending(snapshot.percent)) {
                    Timber.e("board power guard markPending failed, proceed without guard")
                }

                openCtlBoardBlocking()
                // 等待供电完成
                delay(2000)
                openSerialPort()
                // power 板上电后第一次请求可能会CRC报错，先poll一次
                CtlCommandsV2.readAllData(CtlCommandsV2.poll())

                val hiResult = CtlCommandsV2.readAllData(CtlCommandsV2.hi())
                if (!BoardPowerGuard.isValidHiResult(hiResult)) {
                    Timber.w("board power handshake failed, reason=%s, hiResult=%s", reason, hiResult)
                    // 握手失败但进程未崩溃：当前session也要锁主流程，不能让流程
                    // 在主板未确认可用的情况下继续跑；等下次重试握手成功再解锁
                    AppParams.setBoardPowerBlocked(blocked = true, agingWarn = false)
                    return@withLock
                }

                // 启动时静默上报软件版本与固件版本
                reportVersionsSilently(hiResult)

                launch {
                    delay(BoardPowerGuard.STABLE_DELAY_MS)
                    BoardPowerGuard.markConfirmedStable()
                    AppParams.setBoardPowerBlocked(blocked = false, agingWarn = false)
                    Timber.w("board power confirmed stable, reason=%s", reason)
                }
            }
        }
    }

    private suspend fun openCtlBoardBlocking() {
        AppSystemUtils.powerOffCtlBoard()
        delay(500)
        AppSystemUtils.powerOnCtlBoard()
    }

    /**
     * 启动时静默上报软件版本与固件版本。
     *
     * 尽力而为：无 UI 提示，失败仅记录日志。仅在 Nano 流程下执行
     * （此时才配置了上报接口）。
     *
     * hiResult 由调用方（attemptBoardPowerOn）握手成功后传入，避免重复发一次
     * hi() 串口指令。
     */
    private suspend fun reportVersionsSilently(hiResult: String) {
        try {
            val sysConfig = SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
            if (!ConfigSysBean.isNanoFlow(sysConfig.flow)) {
                Timber.d("skip silent version report: not nano flow")
                return
            }

            val firmwareVersion = NanoAuthSupport.extractFirmwareVersion(hiResult)
            val firmwareId = NanoAuthSupport.extractFirmwareId(hiResult)

            // 刷新本地缓存的固件版本
            if (firmwareVersion.isNotBlank()) {
                val configBean =
                    SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoV2Bean::class)
                SysConfigService.saveBean(
                    ConfigInfoBean.PREFIX,
                    configBean.copy(hardware = firmwareVersion)
                )
            }
            // 板上电后刷新本地缓存的 firmware_id，供后续 invalid_comm_token 自动
            // 重新 /activate 时使用，不用等人工去工厂测试页手点激活
            NanoAuthStore.updateFirmwareId(firmwareId)

            // 网络上报后台执行，不阻塞启动
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = NanoApi.uploadLocalMachineInfo(firmwareVersion = firmwareVersion)
                    Timber.d(
                        "silent version report: software=%s firmware=%s ok=%s skipped=%s msg=%s",
                        BuildConfig.VERSION_NAME,
                        firmwareVersion,
                        result.ok,
                        result.skipped,
                        result.message
                    )
                } catch (e: Exception) {
                    Timber.w(e, "silent version report upload failed")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "silent version report failed")
        }
    }
```

- [ ] **Step 3: Remove the redundant `hi()` call inside `openSerialPort()`**

Current `App.kt:202-213`:
```kotlin
    fun openSerialPort() {
        serialHelper = object : SerialHelperV2("/dev/ttyS1", 230400) {}
        serialHelper!!.stopBits = 1
        serialHelper!!.dataBits = 8
        serialHelper!!.parity = 0
        serialHelper!!.flowCon = 0
        serialHelper!!.close()
        serialHelper!!.open()

        val hiResult = CtlCommandsV2.readAllData(CtlCommandsV2.hi())
        Timber.w("hiResult: $hiResult")
    }
```

Replace with:
```kotlin
    fun openSerialPort() {
        serialHelper = object : SerialHelperV2("/dev/ttyS1", 230400) {}
        serialHelper!!.stopBits = 1
        serialHelper!!.dataBits = 8
        serialHelper!!.parity = 0
        serialHelper!!.flowCon = 0
        serialHelper!!.close()
        serialHelper!!.open()
    }
```

(The `hi()` handshake is now always performed by the caller — `attemptBoardPowerOn()` — right after `openSerialPort()` returns, so its result can be validated by `BoardPowerGuard.isValidHiResult()`.)

- [ ] **Step 4: Verify the whole module compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (this also validates Task 4's edits to `AppBatteryReceiverHelper.kt`, which reference `BoardPowerAttemptReason` and `App.attemptBoardPowerOn` defined here)

- [ ] **Step 5: Commit both Task 4 and Task 5 changes together**

```bash
git add app/src/main/java/poct/device/app/App.kt app/src/main/java/poct/device/app/utils/app/AppBatteryReceiverHelper.kt
git commit -m "feat: add attemptBoardPowerOn as the single board power-on entry point"
```

---

### Task 6: Clear the guard flag before intentional restarts

**Files:**
- Modify: `app/src/main/java/poct/device/app/utils/app/AppSystemUtils.kt:53-63` and `:25-32`
- Modify: `app/src/main/java/poct/device/app/component/AppPowerButton.kt:240-248`

**Interfaces:**
- Consumes: `BoardPowerGuard.clearForIntentionalRestartBlocking()` (Task 2)

- [ ] **Step 1: `AppSystemUtils.kt` — `restartApp`, `shutdown`, `reboot`**

Current (`AppSystemUtils.kt:25-32`):
```kotlin
    fun restartApp() {
        val intent = App.getContext().packageManager.getLaunchIntentForPackage(
            App.getContext().packageName
        )
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        App.getContext().startActivity(intent)
        Process.killProcess(Process.myPid())
    }
```

Replace with:
```kotlin
    fun restartApp() {
        BoardPowerGuard.clearForIntentionalRestartBlocking()
        val intent = App.getContext().packageManager.getLaunchIntentForPackage(
            App.getContext().packageName
        )
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        App.getContext().startActivity(intent)
        Process.killProcess(Process.myPid())
    }
```

Current (`AppSystemUtils.kt:50-63`):
```kotlin
    /**
     * 关闭设备
     */
    fun shutdown() {
        runCommand("reboot -p")
    }


    /**
     * 重启设备
     */
    fun reboot() {
        runCommand("reboot")
    }
```

Replace with:
```kotlin
    /**
     * 关闭设备
     */
    fun shutdown() {
        BoardPowerGuard.clearForIntentionalRestartBlocking()
        runCommand("reboot -p")
    }


    /**
     * 重启设备
     */
    fun reboot() {
        BoardPowerGuard.clearForIntentionalRestartBlocking()
        runCommand("reboot")
    }
```

(No new import needed — `BoardPowerGuard` is in the same package `poct.device.app.utils.app`.)

- [ ] **Step 2: `AppPowerButton.kt` — `AppPowerButtonViewModel.restart`**

Current (`AppPowerButton.kt:240-248`):
```kotlin
    fun restart(context: Context) {
        restartConfirm.value = false
        restartIng.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val dpm = getDevicePolicyManager(context)
            val adminComponent = getAdminComponent(context)
            dpm.reboot(adminComponent)
        }
    }
```

Replace with:
```kotlin
    fun restart(context: Context) {
        restartConfirm.value = false
        restartIng.value = true
        viewModelScope.launch(Dispatchers.IO) {
            BoardPowerGuard.clearForIntentionalRestartBlocking()
            val dpm = getDevicePolicyManager(context)
            val adminComponent = getAdminComponent(context)
            dpm.reboot(adminComponent)
        }
    }
```

Add the import near the top of `AppPowerButton.kt` (with the other `poct.device.app.*` imports, e.g. next to `import poct.device.app.MyDeviceAdminReceiver`):
```kotlin
import poct.device.app.utils.app.BoardPowerGuard
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/poct/device/app/utils/app/AppSystemUtils.kt app/src/main/java/poct/device/app/component/AppPowerButton.kt
git commit -m "fix: clear board power guard flag before intentional restarts"
```

---

### Task 7: Screen-on recovery goes through the guard

**Files:**
- Modify: `app/src/main/java/poct/device/app/MainActivity.kt:152-168`

**Interfaces:**
- Consumes: `App.attemptBoardPowerOn`/`BoardPowerAttemptReason` (Task 5)

- [ ] **Step 1: Replace the `ACTION_SCREEN_ON` handler**

Current (`MainActivity.kt:152-168`):
```kotlin
                Intent.ACTION_SCREEN_ON -> {
                    screenOffJob?.cancel()
                    screenOffJob = null
                    setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
                    resetIdleTimer()
                    if (ctlBoardPoweredOff) {
                        ctlBoardPoweredOff = false
                        AppParams.ctlBoardResetEvent.value = System.currentTimeMillis()
                        lifecycleScope.launch(Dispatchers.IO) {
                            Timber.w("screen on — powering on ctl board, reopening serial port")
                            AppSystemUtils.powerOnCtlBoard()
                            App.getContext().openSerialPort()
                            // power 板上电后第一次请求可能会CRC报错，先poll一次
                            CtlCommandsV2.readAllData(CtlCommandsV2.poll())
                        }
                    }
                }
```

Replace with:
```kotlin
                Intent.ACTION_SCREEN_ON -> {
                    screenOffJob?.cancel()
                    screenOffJob = null
                    setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
                    resetIdleTimer()
                    if (ctlBoardPoweredOff) {
                        ctlBoardPoweredOff = false
                        AppParams.ctlBoardResetEvent.value = System.currentTimeMillis()
                        Timber.w("screen on — attempting board power on recovery")
                        App.getContext().attemptBoardPowerOn(BoardPowerAttemptReason.SCREEN_ON_RECOVERY)
                    }
                }
```

(`CtlCommandsV2` import may now be unused in `MainActivity.kt` if this was its only use site — check with the next step before removing the import.)

- [ ] **Step 2: Check whether the `CtlCommandsV2` import is now unused**

Run: `grep -n "CtlCommandsV2" app/src/main/java/poct/device/app/MainActivity.kt`

If the only remaining match is the `import poct.device.app.serial.v2.ctl.CtlCommandsV2` line itself, remove that import line. If there are other usages elsewhere in the file, leave it.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/poct/device/app/MainActivity.kt
git commit -m "fix: route screen-on board power recovery through attemptBoardPowerOn"
```

---

### Task 8: Main-flow lockout overlay

**Files:**
- Create: `app/src/main/java/poct/device/app/component/BoardPowerGuardOverlay.kt`
- Modify: `app/src/main/java/poct/device/app/MainActivity.kt:609-645` (the `MainNavHost` composable)

**Interfaces:**
- Consumes: `AppParams.boardPowerBlocked`/`boardPowerAgingWarn` (Task 3), `R.string.msg_board_power_need_charger`/`msg_board_power_aging_warn` (Task 1), `RouteConfig.SETTING`/`AFTER_SALE`/`SYS_FUN` (existing), `AppFilledButton` (existing, `component` package)
- Produces: `@Composable fun BoardPowerGuardOverlay(navController: NavController)`

- [ ] **Step 1: Create the overlay composable**

```kotlin
package poct.device.app.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.theme.bgColor
import poct.device.app.theme.dangerColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.tipBgColor

/**
 * 主板欠压重启保护：主流程锁定 overlay。
 * 运维路由组（设置/售后/系统功能）显示常驻 banner，不阻断操作；
 * 其余路由组全屏锁定，不可通过点击/返回键关闭，直到主板确认上电成功。
 */
@Composable
fun BoardPowerGuardOverlay(navController: NavController) {
    val blocked by AppParams.boardPowerBlocked.collectAsState()
    val agingWarn by AppParams.boardPowerAgingWarn.collectAsState()
    if (!blocked) return

    val backStackEntry by navController.currentBackStackEntryAsState()
    val groupRoute = backStackEntry?.destination?.parent?.route
    val isOperationalGroup = groupRoute == RouteConfig.SETTING ||
        groupRoute == RouteConfig.AFTER_SALE ||
        groupRoute == RouteConfig.SYS_FUN

    var agingDialogDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(blocked) {
        // 每次进入新的锁定循环，重置老化提示的展示状态
        agingDialogDismissed = false
    }

    if (isOperationalGroup) {
        BoardPowerGuardBanner()
    } else {
        BackHandler(enabled = true) {}
        BoardPowerGuardFullScreenBlock()
    }

    if (agingWarn && !agingDialogDismissed) {
        BoardPowerGuardAgingDialog(onDismiss = { agingDialogDismissed = true })
    }
}

@Composable
private fun BoardPowerGuardFullScreenBlock() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tipBgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(320.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                modifier = Modifier.padding(24.dp),
                text = stringResource(id = R.string.msg_board_power_need_charger),
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = fontColor,
            )
        }
    }
}

@Composable
private fun BoardPowerGuardBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(dangerColor)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.msg_board_power_need_charger),
            color = bgColor,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun BoardPowerGuardAgingDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(280.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.msg_board_power_aging_warn),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    color = fontColor,
                )
                AppFilledButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                    text = stringResource(id = R.string.btn_label_ok),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Wire the overlay into `MainNavHost`**

Current (`MainActivity.kt:609-645`):
```kotlin
@Composable
fun MainNavHost() {
    val navController = rememberNavController()

    // 确保在 NavHost 之前设置 ViewModelStore
    val context = LocalContext.current
    LaunchedEffect(navController) {
        if (context is ComponentActivity) {
            navController.setViewModelStore(context.viewModelStore)
        }
    }

    NavHost(navController = navController, startDestination = RouteConfig.SINGLE) {
        // single路由注册
        singleNav(navController)
        // home路由注册
        homeNav(navController)
        // setting路由注册
        settingNav(navController)
        // work路由注册
        workNav(navController)
        // 报表路由注册
        reportNav(navController)
        // 售后路由注册
        afterSaleNav(navController)
        // 系统功能路由注册
        sysFunNav(navController)
        // 检测配置路由注册
        workConfigNav(navController)
        // 系统配置路由注册
        sysConfigNav(navController)
        // 例子配置路由注册
        sampleConfigNav(navController)
        // 倒计时路由注册
        countdownNav(navController)
    }
}
```

Replace with:
```kotlin
@Composable
fun MainNavHost() {
    val navController = rememberNavController()

    // 确保在 NavHost 之前设置 ViewModelStore
    val context = LocalContext.current
    LaunchedEffect(navController) {
        if (context is ComponentActivity) {
            navController.setViewModelStore(context.viewModelStore)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = RouteConfig.SINGLE) {
            // single路由注册
            singleNav(navController)
            // home路由注册
            homeNav(navController)
            // setting路由注册
            settingNav(navController)
            // work路由注册
            workNav(navController)
            // 报表路由注册
            reportNav(navController)
            // 售后路由注册
            afterSaleNav(navController)
            // 系统功能路由注册
            sysFunNav(navController)
            // 检测配置路由注册
            workConfigNav(navController)
            // 系统配置路由注册
            sysConfigNav(navController)
            // 例子配置路由注册
            sampleConfigNav(navController)
            // 倒计时路由注册
            countdownNav(navController)
        }
        BoardPowerGuardOverlay(navController)
    }
}
```

Add these two imports near the top of `MainActivity.kt` (with the other `androidx.compose.foundation.layout.*` / `poct.device.app.*` imports):
```kotlin
import androidx.compose.foundation.layout.Box
import poct.device.app.component.BoardPowerGuardOverlay
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/poct/device/app/component/BoardPowerGuardOverlay.kt app/src/main/java/poct/device/app/MainActivity.kt
git commit -m "feat: lock main flow with a full-screen overlay when board power is blocked"
```

---

### Task 9: Full build + manual verification pass

**Files:** none (verification only)

- [ ] **Step 1: Full assemble to catch anything the per-task `compileDebugKotlin` checks missed (resource merging, lint-blocking issues, etc.)**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL, APK produced under `app/build/outputs/apk/debug/`

- [ ] **Step 2: Run the full unit test suite**

Run: `./gradlew test`
Expected: all tests pass, including the 9 new `BoardPowerGuardTest` cases from Task 2 and the existing suite unaffected

- [ ] **Step 3: Manual verification checklist (on-device — these scenarios are not covered by JVM unit tests per spec §8; write down actual results, don't just check the box)**

Install the debug APK on a real device (or one with the GPIO paths available) and verify:

- [ ] Fresh install / first boot: app starts normally, board powers on, no lock screen ever appears.
- [ ] Simulated crash loop: after a successful boot, use `adb shell` to directly overwrite the `board_power_guard_pending` row in `tbl_sys_config` to `"true"` (via `adb shell` + `sqlite3` on the app's database, or by killing the process mid-`attemptBoardPowerOn` before the 10s stability timer fires), then restart the app with the device **not** charging. Confirm: board power-on is skipped, full-screen lock appears with "电池电量低，请充电", `setting`/`afterSale`/`sysFun` screens still reachable and show the banner instead.
- [ ] Same as above but set `board_power_guard_battery_at_pending` above 40 before restart: confirm the aging dialog ("电池老化，请联系客服更换电池") also appears once, and doesn't reappear after dismissing it and navigating between screens while still locked.
- [ ] While locked, plug in the charger: confirm board power-on retries automatically, and the lock clears ~10s after a successful handshake.
- [ ] Trigger a reboot from Settings (`AppSystemUtils.reboot()` call site) and from the power button (`AppPowerButtonViewModel.restart()`): confirm the next boot does **not** show the lock screen (flag was cleared before reboot).
- [ ] Screen off for the 30-minute board power-down timeout, then wake the screen: confirm board power-on goes through `attemptBoardPowerOn(SCREEN_ON_RECOVERY)` (check Timber logs for `"screen on — attempting board power on recovery"`) and behaves correctly whether or not a prior lock is active.
- [ ] Rapidly plug/unplug the charger a few times while locked: confirm only one power-on attempt fires per 2-second window (check Timber logs for `"skip board power attempt: debounced"`).

- [ ] **Step 4: Update spec status**

In `docs/superpowers/specs/2026-09-04-board-power-guard-design.md`, change line 4 from:
```
- 状态：待实现
```
to:
```
- 状态：已实现
```

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-09-04-board-power-guard-design.md
git commit -m "docs: mark board power guard spec as implemented"
```
