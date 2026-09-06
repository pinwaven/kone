# 电池老化保护：主板上电重启循环防护 设计文档

- 日期：2026-09-04（2026-09-05 补充实机排查后的修正）
- 状态：已实现，实机排查发现问题并追加修正，待再次上机验证
- 相关代码：`App.kt`、`AppSystemUtils.kt`、`AppBatteryReceiverHelper.kt`、`SysConfig` 相关entity/dao/service、`MainActivity.kt`/`RouteConfig.kt`、`AppPowerButton.kt`、`HomeMain.kt`、`component/BoardPowerGuardOverlay.kt`

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

存储复用现有 `SysConfig` key-value 表，不新建 Room entity；2026-09-05 追加一份 SharedPreferences 快速镜像作为 Room 写入的容灾备份（见 5.2.1）。

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
    const val AGING_BATTERY_THRESHOLD = 40
    private const val BATTERY_RECOVERY_THRESHOLD = 5  // 见 5.2.2

    sealed interface Decision {
        object ProceedNormal : Decision  // 项目实际 Kotlin 语言版本为 1.8，data object 需要1.9+，用 object
        data class BlockNeedCharger(val agingWarn: Boolean) : Decision
    }

    fun decideFrom(pending: Boolean, batteryAtPending: Int, currentBattery: Int, plugged: Boolean?): Decision
    suspend fun decide(rawBattery: Int, plugged: Boolean?): Decision
    suspend fun markPending(rawBattery: Int): Boolean // 每次真正尝试上电前调用，覆盖写入；写入失败返回false
    suspend fun markConfirmedStable()         // 握手成功+稳定期后调用，清 pending
    fun clearForIntentionalRestartBlocking()  // 主动重启/关机前同步清 pending，避免进程马上退出导致未写入
}
```

`decideFrom()` 逻辑（纯函数，不碰数据库，单测直接覆盖）：

| pending | plugged | 电量条件 | 结果 |
|---|---|---|---|
| false | - | - | ProceedNormal |
| true | true | - | ProceedNormal（重试上电） |
| true | null（未知） | - | BlockNeedCharger(agingWarn=false) |
| true | false | currentBattery - batteryAtPending >= 5 | ProceedNormal（见 5.2.2，电量回弹放行） |
| true | false | 否则 | BlockNeedCharger(agingWarn = batteryAtPending > 40) |

若无法读取电量或充电状态：
- `pending=false`：按 `ProceedNormal` 处理，避免首次启动被 guard 自身误锁。
- `pending=true` 且无法确认插电：按未插电阻断处理，`agingWarn=false`，且不参与电量回弹判断（`plugged=null` 直接短路阻断，早于回弹判断）。

#### 5.2.1 快速镜像（SharedPreferences）——2026-09-05 实机排查后追加

**背景**：初版只写 Room（`SysConfigService.saveBean`）。实机排查怀疑：真实欠压重启发生得极快（App启动后1-2秒内），Room 数据库若还没完成打开/迁移，一次简单写入也可能来不及在硬件断电前落盘，导致 `pending` flag 实际没写成功，guard 形同虚设。

**方案**：`markPending()` 先用 `SharedPreferences.edit().commit()`（同步、走 fsync，不依赖 Room 是否已初始化）写一份轻量镜像，再写 Room：

```kotlin
suspend fun markPending(rawBattery: Int): Boolean {
    val fastMarked = markFastPending(rawBattery)  // SharedPreferences commit()，同步落盘
    return try {
        saveDbPending(rawBattery)                 // 原有 Room 写入
        true
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) {
        Timber.e(e, "BoardPowerGuard markPending failed")
        fastMarked  // Room 写失败但快速镜像成功，仍算保护生效
    }
}
```

`decide()` 读取时两边都查，镜像和 DB 只要有一个说 pending 就当 pending，并且发现"镜像有、DB没有"时把 DB 补写回去（便于后续 `decide()` 不必每次都依赖镜像）：

```kotlin
suspend fun decide(rawBattery: Int, plugged: Boolean?): Decision {
    val dbPending = ...
    val fastPending = readFastPending()
    val pending = dbPending || fastPending.pending
    val batteryAtPending = if (dbPending) ... else if (fastPending.pending) fastPending.batteryAtPending else rawBattery
    if (fastPending.pending && !dbPending) {
        try {
            saveDbPending(fastPending.batteryAtPending)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            // 补写失败不能影响本次已经算出的 decision，否则一次异常就会让
            // decide() 整体往外抛，被 App.kt 的 fail-open 逻辑变成误放行
            Timber.e(e, "recover-to-db write failed, keep fast-mirror decision")
        }
    }
    return decideFrom(pending, batteryAtPending, rawBattery, plugged)
}
```

`markConfirmedStable()`/`clearForIntentionalRestartBlocking()` 清 pending 时，Room 和镜像都要清（`clearFastPending()`）。

**已知局限（未来如需再收紧可参考）**：`clearFastPending()` 返回值目前没有校验/重试；且 Room 清除若先抛异常，`clearFastPending()` 不会被执行到——两个存储短暂不同步的窗口比只用单一数据源时更宽。影响方向是"该解锁时多锁一次"（安全但体验差），不是"该锁时没锁住"，本次判断可接受，未来若要收紧可以加返回值校验/重试。

**验证状态**：这是根据"崩溃发生极快"这一现象推导出的工程修复，尚未在实机上正式复测确认——之前的实机测试一度因为设备上装的是不含本功能的旧包而完全失真（详见文档末尾第 11 节），排查过程中被这个问题严重干扰，真正针对新逻辑的复测还需要重新做一轮。

#### 5.2.2 未插电时的电量回弹放行——产品决策，2026-09-05

背景讨论：用户体验诉求是"关机充电一段时间后拔电开机，不应该还提示需要充电"。技术上确认：关机时插着charger再开机，`plugged` 在启动时就会读到 `true`，走 `decide()` 的 `ProceedNormal` 分支，本来就不受此问题影响；真正的场景是"充电后**拔掉充电器**再开机"——这种情况下 `plugged=false`，如果严格按最初设计（只信任 `plugged`），会继续阻断。

产品侧最终决定接受这个折中：**未插电时，若当前电量比写 `pending` 那一刻的电量高出 ≥5 个百分点，视为"电量已回升，允许重试上电"**，即使充电器已经拔掉。

需要认知到的风险（已与产品侧确认，接受）：这个功能本身针对的是"内阻变大的老化电池"——它的问题不是电量不够，而是**瞬间电流输出能力不够**，即使电量涨了，拔电后单靠电池供电仍可能欠压。电量涨 1% 量级的读数完全可能只是老化电池卸载负载后的电压回弹噪声，不代表真的显著充电过；`BATTERY_RECOVERY_THRESHOLD` 定为 5%（而不是 >0 即放行）就是为了把这类噪声误判的概率降低，但这仍然是一个启发式判断，不是精确的"电池现在能不能扛住上电电流"的判断。如果后续实机复测发现 5% 门槛下依然容易被噪声触发导致死循环复现，需要考虑更保守的方案（比如要求电量涨幅在一段最短时间窗口内持续观察到，而不是单次读数比较）。

### 5.3 `App.kt` 改动

在 `onCreate()` 完成基础初始化后，替换原本直接调用 `openCtlBoard()` / `startupService()` 的逻辑。`decide()` 本身可能因为数据库异常而抛出，必须 fail-open（终审发现的问题，2026-09-05 修复）：

```kotlin
val batterySnapshot = AppBatteryReceiverHelper.readRawBatteryOnce(this)
val decision = try {
    BoardPowerGuard.decide(batterySnapshot.percent, batterySnapshot.plugged)
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // decide() 读库失败：降级为放行上电，不因 guard 自身故障把设备锁死
    // （之前版本没有这层保护，读库异常会导致既不上电也不锁 UI）
    Timber.e(e, "board power guard decide() failed, fail open to ProceedNormal")
    BoardPowerGuard.Decision.ProceedNormal
}

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
// 初始为负的防抖窗口而非 0，避免设备开机极快、elapsedRealtime() 还很小时
// 第一次上电尝试被误判为在防抖窗口内而跳过
private var lastBoardPowerAttemptAt = -BoardPowerGuard.RETRY_DEBOUNCE_MS
private var boardPowerAttemptGeneration = 0L

fun attemptBoardPowerOn(reason: BoardPowerAttemptReason) {
    appScope.launch(Dispatchers.IO) {
        boardPowerAttemptMutex.withLock {
            val now = SystemClock.elapsedRealtime()
            // CHARGER_PLUGGED 是真实物理操作，不做防抖，避免刚好卡在上一次
            // 失败尝试的 2 秒窗口内被吞掉
            if (reason != BoardPowerAttemptReason.CHARGER_PLUGGED &&
                now - lastBoardPowerAttemptAt < BoardPowerGuard.RETRY_DEBOUNCE_MS
            ) {
                Timber.w("skip board power attempt: debounced, reason=%s", reason)
                return@withLock
            }
            lastBoardPowerAttemptAt = now

            val batterySnapshot = AppBatteryReceiverHelper.readRawBatteryOnce(this@App)
            // 已处于锁定态且未插电，不自动重试（比如屏幕熄灭超时断电后又唤醒）
            if (AppParams.boardPowerBlocked.value && batterySnapshot.plugged != true) {
                Timber.w("skip board power attempt while blocked and not charging: reason=%s", reason)
                return@withLock
            }

            // generation 必须晚于上面两个空跑的 return@withLock 才自增，见下方说明
            boardPowerAttemptGeneration += 1
            val attemptGeneration = boardPowerAttemptGeneration

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
                boardPowerAttemptMutex.withLock {
                    if (attemptGeneration != boardPowerAttemptGeneration) {
                        // 10秒等待期间又有更新的 attempt 抢先跑了，本次确认已过期，
                        // 不能覆盖更新那次的结果（比如更新那次握手失败正确上了锁，
                        // 这次陈旧的 confirm 不该把它解开）
                        Timber.w("skip stale board power stable confirmation: reason=%s", reason)
                    } else {
                        BoardPowerGuard.markConfirmedStable()
                        AppParams.setBoardPowerBlocked(blocked = false, agingWarn = false)
                        Timber.w("board power confirmed stable, reason=%s", reason)
                    }
                }
            }
        }
    }
}
```

实现注意：
- `openCtlBoardBlocking()` 不再额外创建 `Thread`，在 IO 协程内顺序执行 `powerOffCtlBoard()`、`delay(500)`、`powerOnCtlBoard()`。
- `openSerialPort()` 只负责打开串口，不再吞掉 `hiResult`；握手结果在 `attemptBoardPowerOn()` 中读取并判断。`openSerialPort()` 内部改为先 `serialHelper?.close()` 再创建新实例——`attemptBoardPowerOn()` 可能在同一次 app 运行中被反复调用（插拔充电器、屏幕唤醒），旧版每次都 new 一个新 `SerialHelperV2` 覆盖引用而不关闭上一个，会漏串口底层资源（2026-09-05 修复）。
- `isValidHiResult()` 按 `docs/device-hardware-api.md` 5.2 节文档的协议格式校验（`!|ver:...~<24位十六进制UID>`），不是简单非空判断。
- `startupService()` 中不依赖主板的初始化保留，依赖串口/主板的部分迁入 `attemptBoardPowerOn()`。
- **generation 计数器**（2026-09-05 新增，修复终审发现的结构性隐患）：10 秒稳定确认协程在 `withLock` 释放后才跑，理论上可能和一次新的并发 attempt 产生竞态——如果旧的确认在新 attempt 已经改变了状态之后才执行，会用过期结果覆盖新结果。用一个自增计数器标记"这是第几次真正的上电尝试"，稳定确认执行前重新加锁校验 generation 没变过，变了就放弃这次确认。generation 只应该在真正碰到硬件的路径上自增，不能在前面两个空跑的 `return@withLock` 里加（曾经踩过这个坑：generation 加得太早，会导致一次没有实际动作的空跑调用，错误地使前一个正在跑的真实 attempt 的确认失效，造成"明明上电成功了却一直不解锁"）。

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

- 当前路由属于运维组（`setting`/`afterSale`/`sysFun`）：不做全屏拦截，显示常驻 banner 提示"电池电量过低，无法完成检测，请充电"（+老化提示见下）。
- 其余所有路由组（`countdown`/`single`/`home`/`work`/`report`/`workConfig`/`sysConfig`/`sample`）：显示同样文案的提示框。

**提示框可关闭（2026-09-05 由不可关闭改为可关闭）**：最初设计是全屏非 dismiss 弹窗，点击/返回键均不可关闭，实测体验很差（用户反馈：一直卡着全屏遮罩，什么都看不到）。改为：

- 提示框加一个"确定"按钮，点击后关闭；物理返回键效果等同点击确定（也是关闭，不再是原来的吞掉不响应）。
- **关闭只是把提示框收起，`boardPowerBlocked` 状态本身不受影响**——不代表"已解决"，不会让用户借此绕开保护真正进入 Work 流程操作。
- `AppParams` 新增 `boardPowerBlockReassertEvent: MutableStateFlow<Long>` 和 `reassertBoardPowerBlock()`。`HomeMain.kt` 首页"开始检测"按钮的 `onClick` 最前面加一道检查：如果 `boardPowerBlocked.value` 仍为 true，调用 `reassertBoardPowerBlock()` 并直接 return，不进入 Work 流程。
- Overlay 内部 `LaunchedEffect(blocked, reassertEvent)` 同时监听两个信号，任一变化都把"是否已关闭"的本地状态重置为未关闭，即重新弹出——保证每次锁定循环开始、或用户尝试真正开始检测被拦下时，提示都会再次出现。

老化提示（`boardPowerAgingWarn == true` 时追加）："电池老化，请联系客服更换电池"。**每次锁定循环只弹一次**（即从 `boardPowerBlocked` 变为 true 到变回 false 算一个循环，循环内只展示一次，不随用户来回切页面反复弹出；用一个"本轮循环是否已展示过"的临时状态位，随 `boardPowerBlocked`/`reassertEvent` 变化而复位）。

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
- **`decide()` 自身读库异常**：`App.kt` 调用处 fail-open 为 `ProceedNormal`（2026-09-05 新增，见 5.3），避免"既不上电也不锁 UI"的更差状态。
- **`decide()` 内部补写 Room 失败**（镜像有 pending 但 DB 没有时的补写）：只记日志，不让异常向外传播，避免污染本次已经算出的 decision（见 5.2.1）。
- **10 秒稳定确认与新 attempt 竞态**：用 generation 计数器识别过期确认并丢弃，避免旧确认覆盖新结果（见 5.3）。
- **未插电但电量回弹 ≥5% 被放行后仍然欠压重启**：这是已知接受的风险（见 5.2.2），不是 bug；若复测发现频繁复现，需要收紧判断（如要求持续观察而非单次读数比较）。

## 8. 测试计划

- `BoardPowerGuardTest.kt`（JVM 单测，`app/src/test/`，共 13 个用例）覆盖 `decideFrom()` 纯函数分支：
  - 无 pending → ProceedNormal
  - pending + 插电 → ProceedNormal
  - pending + 电池状态未知 → BlockNeedCharger(agingWarn=false)
  - pending + 未插电 + 电量≤40 → BlockNeedCharger(agingWarn=false)
  - pending + 未插电 + 电量>40 → BlockNeedCharger(agingWarn=true)
  - pending + 未插电 + 电量回弹刚好=5% → ProceedNormal
  - pending + 未插电 + 电量回弹4%/1%/0% → 仍 BlockNeedCharger（不足5%门槛）
  - `isValidHiResult()` 空/合法/无`ver:`标记/UID长度不对四种输入
  - 涉及 SharedPreferences 快速镜像和 Room 的 `decide()`/`markPending()`/`markConfirmedStable()` 本身未被自动化测试覆盖（沿用项目现有约定：只对纯逻辑函数写JVM单测，Android框架/存储交互部分靠手动或实机验证），第 11 节记录的实机复测就是补这块。
- 模拟崩溃场景（instrumented 或手动）：`markPending()` 后不调用 `markConfirmedStable()` 直接杀进程重启两次，验证第二次启动进入锁定态、提示正确显示。
- 验证主动重启路径（设置里触发 `reboot()`）后，下次开机不会误判为崩溃循环。
- 验证 `AppPowerButton` 直接 `DevicePolicyManager.reboot()` 后，下次开机不会误判为崩溃循环。
- 验证运维路由组（设置/售后）在锁定态下仍可进入，且 banner 正确显示。
- 验证屏幕熄灭超时断电后，屏幕点亮恢复上电走 `attemptBoardPowerOn(SCREEN_ON_RECOVERY)`，不会绕过 guard。
- 验证充电器快速插拔时，2 秒内只触发一次上电尝试。
- 验证握手失败但进程未重启时，不清除 pending，不解锁主流程。

## 9. 已确认的默认值

- 稳定期时长：10 秒
- 弹窗文案："电池电量过低，无法完成检测，请充电"（2026-09-05 由"电池电量低，请充电"改）
- 老化提示："电池老化，请联系客服更换电池"，每个锁定循环只弹一次
- 老化判断阈值：写 flag 时原始电量 > 40%
- 自动重试防抖：2 秒
- 电量回弹放行阈值：未插电时电量比写 flag 时高 ≥5 个百分点才放行重试（2026-09-05 新增，见 5.2.2）
- 锁定提示框：可关闭（2026-09-05 由不可关闭改为可关闭，见 5.5），关闭不等于解除锁定

## 10. 待实现时留意的开放问题

- `hiResult` 的有效性标准需要结合真实固件返回格式最终确认。最低要求非空，更推荐解析出固件版本或 firmware_id。
- `poll` 与 `hi` 的先后顺序需按实机验证结果定。无论顺序如何，清 pending 只能发生在有效握手之后。
- 若后续增加新的主板上电入口，必须走 `App.attemptBoardPowerOn()`，不能直接调用 `AppSystemUtils.powerOnCtlBoard()`。
- **已知例外**：`ui/sample/SampleSerial.kt` 的 `powerOn()`/`powerOff()`（工程诊断页手动电源开关）已直接调用 `AppSystemUtils.powerOnCtlBoard()`/`powerOffCtlBoard()`，绕开本次新入口。这是人工在场的工程诊断操作，不纳入本次 guard 范围，本次不改动；后续如有人对照上面这条原则想"顺手"把它也改造成走 `attemptBoardPowerOn()`，需注意会引入 mutex/debounce，可能影响工程模式手动连续开关测试的体验，改前需单独评估。

## 11. 实机排查记录（2026-09-05）

功能merge进 `dev` 后第一次上机测试反馈"完全没生效，一直重启，`board_power_guard_batteryAtPending` 一直是 -1"。排查过程记录如下，供以后类似问题参考。

### 11.1 一开始走了不少弯路

- `batteryAtPending` 恒为 -1 一开始被当成 bug，后来查清是虚惊：这个字段设计上只有"当前有崩溃循环 pending 中"才有意义，`markConfirmedStable()` 每次成功确认后会把它重置回 -1，看到 -1 也可能只是"最近一次是正常确认稳定"的正常状态，不代表没生效。
- 中途怀疑过 sticky broadcast 在设备刚重启时读不到、Room 数据库初始化时机、SQLite 因硬断电损坏等多种假设，靠 `adb logcat`/root shell 查 `tbl_sys_config` 实测排查，网络 adb 在设备真实重启时会断线重连，取证效率很低。
- **最终定位到关键根因**：测试机上实际安装的 APK **不是**带这个 feature 的新包（`unzip` 解包检查 `classes*.dex` 完全搜不到 `BoardPowerGuard` 相关类/字符串），而 `tbl_sys_config` 里的 `board_power_guard_*` 行是之前某次装过带 feature 的包时留下的脏数据（Room 数据库不会因为重装/降级 APK 被清掉）。**装错包**导致前面几轮测试结果全部不能反映真实代码行为，是这次排查里最大的一次教训。
- 重新用 `./gradlew app:assembleRelease` 打正确签名的 release 包装上去之后，才是针对真实代码的有效测试。

### 11.2 真实复现结果与后续修正

装对包、不插电反复重启测试后，确认了两件事：
1. 不插电时点击 `SampleSerial.kt` 工程诊断页的"关闭供电"/"开启供电"按钮（完全绕开本 feature 的手动测试路径）也会导致整机重启——独立证实了硬件层面确实存在"老化电池+GPIO瞬间上电电流"导致欠压重启的问题，不是代码臆想的场景。
2. 装对包后，仍然观测到不插电反复重启没有被拦住的现象——促成了 5.2.1 节的 SharedPreferences 快速镜像修正（怀疑 Room 写入来不及落盘）和 5.3 节的 fail-open / generation 计数器修正。

这几处修正目前**还没有在实机上完成针对新代码的正式复测**——排查过程被"装错包"这件事严重干扰了测试节奏，真正意义上"确认新逻辑生效"的复测还需要重新走一遍：不插电反复触发崩溃循环，确认第二次开机起被正确拦住、停在锁定提示，且 `board_power_guard_pending`（含 SharedPreferences 镜像）在真实断电场景下能可靠写入。

### 11.3 用户体验反馈驱动的后续调整

实机测试期间还收到两条产品体验反馈，均已采纳并落实到本文档对应章节：
- 锁定提示框改为可关闭（5.5 节），但关闭不等于解除锁定，真正尝试开始检测时会重新拦下来。
- 未插电但电量比记录时上涨的场景放宽为允许重试（5.2.2 节），门槛定为 ≥5%，是在"用户体验诉求"和"电量噪声可能导致死循环复现的风险"之间做的折中，已与产品侧确认接受该风险。
