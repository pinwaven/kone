# 电池老化保护：主板上电重启循环防护 设计文档

- 日期：2026-09-04
- 状态：已实现
- 相关代码：`App.kt`、`AppSystemUtils.kt`、`AppBatteryReceiverHelper.kt`、`SysConfig` 相关entity/dao/service、`MainActivity.kt`/`RouteConfig.kt`、`AppPowerButton.kt`

## 1. 背景与问题

部分设备电池老化（内阻变大），开机瞬间给主板 GPIO 上电时电流冲击导致主控 SoC 欠压重启。若设备未插充电器，会陷入"开机→上电→重启→开机→..."的死循环，用户无法进入任何界面，也无法自行诊断。

## 2. 目标

- 检测到"上次开机上电后未确认稳定"时，本次开机若未插充电器，跳过给主板上电，避免再次触发欠压重启，打破循环。
- 提示用户插入充电器才能继续。
- 若写flag时电量已经不低（>40%）却仍然触发了保护，判断为电池老化而非单纯电量不足，额外提示联系客服换电池。
- 插入充电器后自动重试上电，成功且稳定后自动解锁主流程。
- 用户忽略提示、绕开锁定界面进入主流程的情况下，只要主板未上电，主流程内也要持续提示插电，流程无法继续。

## 3. 非目标

- 不解决"插了充电器但供电依旧不足以支撑上电"的硬件问题（此时会持续重试+锁定，属预期行为，非软件bug）。
- 不做电池健康的长期趋势记录/上报（如需要，后续可扩展，本次不做）。
- 不改变现有电量显示平滑/锁定逻辑（`AppBatteryUtils`），本方案单独读取原始电量。

## 4. 总体架构

Guard 逻辑集中在五处，不新增 Activity/Screen：

1. `App.onCreate()` — 开机上电前的判断与flag写入
2. `App.attemptBoardPowerOn(reason)` — 所有主板上电/串口启动/稳定确认的唯一入口
3. `AppBatteryReceiverHelper` — 监听充电器插入后触发一次受控重试
4. `AppSystemUtils` / `AppPowerButton` — 主动重启、关机、应用重启前同步清flag，避免误判
5. `MainActivity.kt` 的 `MainNavHost` 级别 — 全局锁定 overlay，观察 `AppParams` 状态

存储复用现有 `SysConfig` key-value 表，不新建 Room entity。

## 5. 组件详情

### 5.1 存储：`BoardPowerGuardBean`

现有 `SysConfigService.findBean`/`saveBean` 反射映射模式只支持 `ConfigBean` 的 `String` 属性，不能直接存 `Boolean`/`Int`。因此本方案新增的配置 bean 使用字符串字段，类型转换集中在 `BoardPowerGuard` 内部：

```kotlin
data class BoardPowerGuardBean(
    var pending: String = "false",
    var batteryAtPending: String = "-1",
) : ConfigBean {
    companion object {
        const val PREFIX = "board_power_guard_"
    }
}
```

字段说明：
- `pending`：上一次尝试给主板上电后，是否已确认稳定（握手成功+稳定期过）。`"true"`=未确认，即"危险状态"。
- `batteryAtPending`：写 `pending="true"` 那一刻的**原始电量**（`BatteryManager.EXTRA_LEVEL`/`EXTRA_SCALE` 直接计算，非 `AppBatteryUtils` 平滑/锁定后的显示值），用于后续 >40% 老化判断。
- 若未来不希望继续扩大 `ConfigBean` 的字符串约束，也可新增 `SysConfigService.findValue/saveValue` 专用 key-value 方法；本次优先保持最小改动。

### 5.2 新工具类 `BoardPowerGuard`（`utils/app/BoardPowerGuard.kt`）

```kotlin
object BoardPowerGuard {
    const val STABLE_DELAY_MS = 10_000L  // 稳定期，默认10秒，已确认
    const val RETRY_DEBOUNCE_MS = 2_000L

    sealed interface Decision {
        data object ProceedNormal : Decision
        data class BlockNeedCharger(val agingWarn: Boolean) : Decision
    }

    suspend fun decide(rawBattery: Int, plugged: Boolean): Decision
    suspend fun markPending(rawBattery: Int): Boolean // 每次真正尝试上电前调用，覆盖写入；写入失败返回false
    suspend fun markConfirmedStable()         // 握手成功+稳定期后调用，清 pending
    fun clearForIntentionalRestartBlocking()  // 主动重启/关机前同步清 pending，避免进程马上退出导致未写入
}
```

`decide()` 逻辑：

| pending | plugged | 结果 |
|---|---|---|
| false | - | ProceedNormal |
| true | true | ProceedNormal（重试上电） |
| true | false | BlockNeedCharger(agingWarn = batteryAtPending > 40) |

若无法读取电量或充电状态：
- `pending=false`：按 `ProceedNormal` 处理，避免首次启动被 guard 自身误锁。
- `pending=true` 且无法确认插电：按未插电阻断处理，`agingWarn=false`。

### 5.3 `App.kt` 改动

在 `onCreate()` 完成基础初始化后，替换原本直接调用 `openCtlBoard()` / `startupService()` 的逻辑：

```kotlin
val batterySnapshot = AppBatteryReceiverHelper.readRawBatteryOnce(this)
val decision = BoardPowerGuard.decide(
    rawBattery = batterySnapshot.percent,
    plugged = batterySnapshot.plugged,
)

when (decision) {
    is BoardPowerGuard.Decision.BlockNeedCharger -> {
        AppParams.setBoardPowerBlocked(blocked = true, agingWarn = decision.agingWarn)
        // 跳过 openCtlBoard() 和 startupService() 中涉及主板/串口的部分
        // 其余不依赖主板的初始化（数据库、多语言等）正常执行
    }
    BoardPowerGuard.Decision.ProceedNormal -> {
        attemptBoardPowerOn(reason = BoardPowerAttemptReason.APP_START)
    }
}
```

新增统一上电入口，所有路径只能调用它，不再直接散落调用 `powerOnCtlBoard()` + `openSerialPort()`：

```kotlin
enum class BoardPowerAttemptReason {
    APP_START,
    CHARGER_PLUGGED,
    SCREEN_ON_RECOVERY,
}

private val boardPowerAttemptMutex = Mutex()
private var lastBoardPowerAttemptAt = 0L

fun attemptBoardPowerOn(reason: BoardPowerAttemptReason) {
    appScope.launch(Dispatchers.IO) {
        boardPowerAttemptMutex.withLock {
            val now = SystemClock.elapsedRealtime()
            if (now - lastBoardPowerAttemptAt < BoardPowerGuard.RETRY_DEBOUNCE_MS) {
                Timber.w("skip board power attempt: debounced, reason=%s", reason)
                return@withLock
            }
            lastBoardPowerAttemptAt = now

            val batterySnapshot = AppBatteryReceiverHelper.readRawBatteryOnce(this@App)
            if (!BoardPowerGuard.markPending(batterySnapshot.percent)) {
                Timber.e("board power guard markPending failed, proceed without guard")
            }

            openCtlBoardBlocking()
            openSerialPort()
            // power 板上电后第一次请求可能会CRC报错，先poll一次
            CtlCommandsV2.readAllData(CtlCommandsV2.poll())

            val hiResult = CtlCommandsV2.readAllData(CtlCommandsV2.hi())
            if (!isValidHiResult(hiResult)) {
                Timber.w("board power handshake failed, keep pending flag")
                // 握手失败但进程未崩溃：当前session也要锁主流程，不能让流程
                // 在主板未确认可用的情况下继续跑；等下次重试握手成功再解锁
                AppParams.setBoardPowerBlocked(blocked = true, agingWarn = false)
                return@withLock
            }

            launch {
                delay(BoardPowerGuard.STABLE_DELAY_MS)
                BoardPowerGuard.markConfirmedStable()
                AppParams.setBoardPowerBlocked(blocked = false, agingWarn = false)
                Timber.w("board power confirmed stable, reason=%s", reason)
            }
        }
    }
}
```

实现注意：
- `openCtlBoardBlocking()` 不再额外创建 `Thread`，在 IO 协程内顺序执行 `powerOffCtlBoard()`、`delay(500)`、`powerOnCtlBoard()`。
- `openSerialPort()` 只负责打开串口，不再吞掉 `hiResult`；握手结果在 `attemptBoardPowerOn()` 中读取并判断。
- `isValidHiResult()` 至少要求非空；更建议复用 `NanoAuthSupport.extractFirmwareVersion/Id` 判断 `hi` 格式有效。
- `startupService()` 中不依赖主板的初始化保留，依赖串口/主板的部分迁入 `attemptBoardPowerOn()`。

### 5.4 充电后自动重试

`AppBatteryReceiverHelper.batteryStateReceiver` 收到 `plugged` 从 false→true 变化、且当前 `AppParams.boardPowerBlocked.value == true` 时，触发：

```kotlin
App.getContext().attemptBoardPowerOn(BoardPowerAttemptReason.CHARGER_PLUGGED)
```

实现要求：
- `batteryStateReceiver` 需要记录上一次 `plugged` 状态，只在 false→true 边沿触发，不在每个 battery broadcast 重复触发。
- 重试节流由 `attemptBoardPowerOn()` 内部统一处理，避免插拔抖动导致短时间重复上电。
- 插了充电器但仍欠压重启时，pending 不会被清除；下次启动仍会阻断并等待插电。

### 5.5 主流程锁定 UI

`AppParams` 新增：
```kotlin
val boardPowerBlocked = MutableStateFlow(false)
val boardPowerAgingWarn = MutableStateFlow(false)

fun setBoardPowerBlocked(blocked: Boolean, agingWarn: Boolean) {
    boardPowerBlocked.value = blocked
    boardPowerAgingWarn.value = blocked && agingWarn
}
```

`MainActivity.kt` 的 `MainNavHost` 级别加一层全局 overlay，使用 `collectAsState()` 观察上述状态：

- 当前路由属于运维组（`setting`/`afterSale`/`sysFun`）：不做全屏拦截，显示常驻 banner 提示"电池电量低，请充电"（+老化提示见下）。
- 其余所有路由组（`countdown`/`single`/`home`/`work`/`report`/`workConfig`/`sysConfig`/`sample`）：全屏非 dismiss 弹窗拦截，文案"电池电量低，请充电"。用户点击/返回键均不可关闭，只有 `boardPowerBlocked` 变为 false 才消失。

老化提示（`boardPowerAgingWarn == true` 时追加）："电池老化，请联系客服更换电池"。**每次锁定循环只弹一次**（即从 `boardPowerBlocked` 变为 true 到变回 false 算一个循环，循环内只展示一次，不随用户来回切页面反复弹出；具体可用一个"本轮循环是否已展示过"的临时状态位，随 `boardPowerBlocked` 复位而复位）。

### 5.6 屏幕唤醒恢复上电

当前 `MainActivity.screenReceiver` 在屏幕熄灭超时后会关闭主板电源，屏幕点亮时直接 `powerOnCtlBoard()` + `openSerialPort()`。该路径同样可能触发老化电池欠压重启，必须纳入 guard。

改动要求：
- 屏幕关闭超时主动执行 `powerOffCtlBoard()` 前，不需要写 pending；这是主动下电，不是危险上电。
- 屏幕点亮且 `ctlBoardPoweredOff == true` 时，不直接调用 `AppSystemUtils.powerOnCtlBoard()` / `App.getContext().openSerialPort()`，改为：

```kotlin
App.getContext().attemptBoardPowerOn(BoardPowerAttemptReason.SCREEN_ON_RECOVERY)
```

- 如果当前处于 `boardPowerBlocked=true` 且未插电，屏幕点亮不自动重试；等待充电器插入事件。

### 5.7 主动重启清 flag

`AppSystemUtils.reboot()`、`shutdown()`、`restartApp()` 三处，在执行系统命令前调用 `BoardPowerGuard.clearForIntentionalRestartBlocking()`，避免运维手动重启或固件升级触发的重启被误判为崩溃循环。

另外，`AppPowerButtonViewModel.restart()` 直接调用 `DevicePolicyManager.reboot()`，也必须在调用前同步清 flag：

```kotlin
BoardPowerGuard.clearForIntentionalRestartBlocking()
dpm.reboot(adminComponent)
```

`clearForIntentionalRestartBlocking()` 要保证写入尽量在进程退出/系统重启前完成，可用 `runBlocking(Dispatchers.IO)` 包裹短耗时 DB 写入，并记录失败日志。

## 6. 数据流示例（一次完整崩溃循环）

```
开机N:   无 pending → markPending(battery=X) → 尝试上电 → SoC欠压重启（flag 未清）
开机N+1: pending=true, 读到未插电 → 跳过上电 → boardPowerBlocked=true,
         agingWarn=(X>40) → 锁主流程/运维组显示banner，提示插电（+老化提示如适用）
用户插电: batteryStateReceiver 检测 plugged false→true → attemptBoardPowerOn(CHARGER_PLUGGED)
         → markPending(新电量) → 尝试上电 → 握手成功 → 等10秒稳定
         → markConfirmedStable() → boardPowerBlocked=false → 解锁
```

## 7. 异常处理

- **flag 写入 DB 失败**：降级为放行上电，不因 guard 自身故障把设备锁死；记录 Timber 错误日志。
- **插了充电器但供电依旧不足，反复重启**：guard 会持续重试+锁定，这是预期行为（硬件问题，超出本方案范围）。
- **稳定期内用户又拔掉充电器**：若之后确实又崩溃，下次开机 `pending` 仍为 true，能正确重新触发保护；若没崩溃只是拔线，`markConfirmedStable()` 正常执行，flag 照常清除（不因拔线本身而认为异常）。
- **首次安装/首次开机**：无历史 flag，走 `ProceedNormal` 正常路径。
- **握手失败但未崩溃**：保留 `pending=true`，且当前session立即 `setBoardPowerBlocked(true, agingWarn=false)`，界面同样锁定，不允许主流程在主板未确认可用时继续；后续插电/屏幕恢复/重启后再按 guard 决策重试解锁。
- **重复插拔充电器**：只在 false→true 边沿触发，并由 `attemptBoardPowerOn()` 做 2 秒防抖和互斥。

## 8. 测试计划

- `BoardPowerGuardTest.kt`（JVM 单测，`app/src/test/`）覆盖 `decide()` 四种分支：
  - 无 pending → ProceedNormal
  - pending + 插电 → ProceedNormal
  - pending + 未插电 + 电量≤40 → BlockNeedCharger(agingWarn=false)
  - pending + 未插电 + 电量>40 → BlockNeedCharger(agingWarn=true)
  - pending + 电池状态未知 → BlockNeedCharger(agingWarn=false)
- 模拟崩溃场景（instrumented 或手动）：`markPending()` 后不调用 `markConfirmedStable()` 直接杀进程重启两次，验证第二次启动进入锁定态、提示正确显示。
- 验证主动重启路径（设置里触发 `reboot()`）后，下次开机不会误判为崩溃循环。
- 验证 `AppPowerButton` 直接 `DevicePolicyManager.reboot()` 后，下次开机不会误判为崩溃循环。
- 验证运维路由组（设置/售后）在锁定态下仍可进入，且 banner 正确显示。
- 验证屏幕熄灭超时断电后，屏幕点亮恢复上电走 `attemptBoardPowerOn(SCREEN_ON_RECOVERY)`，不会绕过 guard。
- 验证充电器快速插拔时，2 秒内只触发一次上电尝试。
- 验证握手失败但进程未重启时，不清除 pending，不解锁主流程。

## 9. 已确认的默认值

- 稳定期时长：10 秒
- 弹窗文案："电池电量低，请充电"
- 老化提示："电池老化，请联系客服更换电池"，每个锁定循环只弹一次
- 老化判断阈值：写 flag 时原始电量 > 40%
- 自动重试防抖：2 秒

## 10. 待实现时留意的开放问题

- `hiResult` 的有效性标准需要结合真实固件返回格式最终确认。最低要求非空，更推荐解析出固件版本或 firmware_id。
- `poll` 与 `hi` 的先后顺序需按实机验证结果定。无论顺序如何，清 pending 只能发生在有效握手之后。
- 若后续增加新的主板上电入口，必须走 `App.attemptBoardPowerOn()`，不能直接调用 `AppSystemUtils.powerOnCtlBoard()`。
- **已知例外**：`ui/sample/SampleSerial.kt` 的 `powerOn()`/`powerOff()`（工程诊断页手动电源开关）已直接调用 `AppSystemUtils.powerOnCtlBoard()`/`powerOffCtlBoard()`，绕开本次新入口。这是人工在场的工程诊断操作，不纳入本次 guard 范围，本次不改动；后续如有人对照上面这条原则想"顺手"把它也改造成走 `attemptBoardPowerOn()`，需注意会引入 mutex/debounce，可能影响工程模式手动连续开关测试的体验，改前需单独评估。
