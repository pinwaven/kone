package poct.device.app.serial.v2.ctl

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlinx.coroutines.delay
import poct.device.app.App
import poct.device.app.serial.SerialQueryParams
import poct.device.app.serial.ctl.CtlConstants
import poct.device.app.serial.v2.CtlSerialMessageV2
import poct.device.app.serial.v2.utils.SocketSidUtils
import timber.log.Timber

/**
 * 轮询等待结果
 */
sealed interface WaitResult {
    object Completed : WaitResult
    data class DeviceError(val raw: String) : WaitResult
    object Timeout : WaitResult
}

/**
 * 指令生成帮助类
 */
object CtlCommandsV2 {
    val EMPTY = CtlSerialMessageV2()

    const val delayMs: Long = 150

    // 电机轴：0=水平轴（片仓进出/关门）1=垂直轴（顶针升降）
    private const val MOTOR_H = 0
    private const val MOTOR_V = 1

    // moveToSs 的 ssId：0=向内 1=向外
    private const val SS_IN = 0
    private const val SS_OUT = 1

    // 机械运动参数（velocity/duration）。安全关键：数值集中命名，勿散落于各动作。
    private const val MOVE_OUT_VELOCITY = -70000
    private const val MOVE_OUT_DURATION = 11000
    private const val MOVE_IN_VELOCITY = 70000
    private const val MOVE_IN_DURATION = 11000
    private const val CLOSE_DOOR_VELOCITY = 70000
    private const val CLOSE_DOOR_DURATION = 1000
    private const val MOVE_IN_2MM_VELOCITY = 70000
    private const val MOVE_IN_2MM_DURATION = 250
    private const val MOVE_UP_VELOCITY = 60000
    private const val MOVE_UP_DURATION = 1000
    private const val MOVE_DOWN_VELOCITY = -50000
    private const val MOVE_DOWN_DURATION = 2500
    private const val MOVE_OUT_LITTLE_VELOCITY = -50000
    private const val MOVE_OUT_LITTLE_DURATION = 400
    private const val MOVE_IN_LITTLE_VELOCITY = 50000
    private const val MOVE_IN_LITTLE_DURATION = 100

    // GPIO 应答中表示片仓有卡的标记
    private const val GPIO_HAS_CARD_TOKEN = "ss_card:0"

    // QR 应答里成功数据段的分隔标记
    private const val QR_COMPLETED_TOKEN = "COMPLETED-QR"

    // 各类动作的轮询超时时间：超时后停止轮询并发送取消指令，避免卡死后续指令
    const val MOVE_TIMEOUT_MS = 30_000L
    const val ABSORB_TIMEOUT_MS = 300_000L
    const val SCAN_TIMEOUT_MS = 60_000L
    const val HOMING_TIMEOUT_MS = 60_000L
    const val READ_QR_TIMEOUT_MS = 30_000L

    // 单次读取串口应答的默认超时（毫秒）。慢指令可传入更大的值。
    const val READ_TIMEOUT_MS = 1000

    // 串口收发互斥锁：保证每次 drain+send+read 事务原子，防止并发轮询交错读到别条指令的应答。
    // 用阻塞锁而非协程 Mutex——底层串口读本身阻塞，且都跑在 IO 线程上。
    private val serialLock = Any()

    /**
     * 系统状态轮询
     */
    fun poll(): CtlSerialMessageV2 = command(CtlConstantsV2.CMD_POLL)

    /**
     * 心跳
     */
    fun hi(): CtlSerialMessageV2 = command(CtlConstantsV2.CMD_HI)

    /**
     * 读取GPIO状态
     */
    fun gpioRead(): CtlSerialMessageV2 = command(CtlConstantsV2.CMD_GPIO_READ)

    /**
     * 读取GPIO状态（是否存在卡片）
     */
    fun gpioReadHasCard(result: String): Boolean {
        return result.contains(GPIO_HAS_CARD_TOKEN)
    }

    /**
     * 取消当前动作
     */
    fun cancel(): CtlSerialMessageV2 = command(CtlConstantsV2.CMD_ACTION_CANCEL)

    /**
     * 归零操作
     */
    fun homing(): CtlSerialMessageV2 = command(CtlConstantsV2.CMD_ACTION_HOMING)

    /**
     * 通用轮询骨架：每隔 delayMs 轮询 poll()，[handle] 返回非 null 即结束并返回该值，
     * 返回 null 继续轮询。[keepWaiting] 返回 false 时以 [onStop] 结果提前结束；
     * 超时返回 [onTimeout] 结果。空应答帧跳过。delay 可被取消。
     */
    private suspend fun <T> pollLoop(
        tag: String,
        timeoutMs: Long,
        onTimeout: () -> T,
        keepWaiting: () -> Boolean = { true },
        onStop: () -> T = onTimeout,
        handle: (result: String) -> T?,
    ): T {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime <= timeoutMs) {
            if (!keepWaiting()) return onStop()

            val result = readAllData(poll())
            if (result.isNotEmpty()) {
                Timber.d("%s result: %s", tag, result)
                handle(result)?.let { return it }
            }

            delay(delayMs)
        }
        return onTimeout()
    }

    /**
     * 轮询等待归零完成；delay 可被取消，取消后轮询立即停止
     * @param onProgress 进度回调（HOMING_STATUS_MAP 中的进度值）
     * @return true 表示归零完成，false 表示出错或超时
     */
    suspend fun waitHomingStatusSuccess(
        timeoutMs: Long = HOMING_TIMEOUT_MS,
        onProgress: (progressVal: Int) -> Unit = {},
    ): Boolean = pollLoop(
        tag = "waitHomingStatusSuccess",
        timeoutMs = timeoutMs,
        onTimeout = {
            Timber.e("waitHomingStatusSuccess timeout ${timeoutMs}ms, sending cancel")
            readAllData(cancel())
            false
        },
    ) { result ->
        if (isSuccess(result)) {
            var decision: Boolean? = null
            for (key in CtlConstantsV2.HOMING_STATUS_MAP.keys) {
                if (result.contains("s:$key")) {
                    val progressVal = CtlConstantsV2.HOMING_STATUS_MAP.getValue(key)
                    Timber.w("homing status: $key progressVal: $progressVal")
                    onProgress(progressVal)
                    if (progressVal >= CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED) {
                        decision = true
                    }
                    break
                }
            }
            decision
        } else if (result.startsWith(CtlConstantsV2.RESULT_ERROR_PREFIX)) {
            Timber.e("waitHomingStatusSuccess device error: $result")
            false
        } else {
            null
        }
    }

    /**
     * 定时运动
     */
    fun moveDuration(motorId: Int, velocity: Int, duration: Int): CtlSerialMessageV2 =
        command(
            CtlConstantsV2.CMD_ACTION_MOVE_DURATION,
            byteArrayOf(motorId.toByte()) + velocity.toLeBytes() + duration.toLeBytes()
        )

    /**
     * 通用轮询等待：完成、设备出错或超时必定返回，不会永久阻塞后续指令。
     * delay 可被取消：取消指令后轮询立即停止；超时后发送取消指令，避免设备停留在动作中。
     */
    private suspend fun waitStatus(completedToken: String, timeoutMs: Long): WaitResult = pollLoop(
        tag = "waitStatus",
        timeoutMs = timeoutMs,
        onTimeout = {
            Timber.e("waitStatus timeout ${timeoutMs}ms waiting $completedToken, sending cancel")
            readAllData(cancel())
            WaitResult.Timeout
        },
    ) { result ->
        if (isSuccess(result)) {
            // 先判错误再判完成：同一帧可能同时携带完成与错误标记（如多轴分别上报），
            // 出错必须优先判为失败并触发归零重试，绝不能被 COMPLETED 子串掩盖当成成功。
            when {
                result.contains(CtlConstantsV2.CMD_ACTION_STATUS_ERROR) -> {
                    Timber.e("waitStatus device error: $result")
                    WaitResult.DeviceError(result)
                }
                result.contains(completedToken) -> WaitResult.Completed
                else -> null
            }
        } else if (result.startsWith(CtlConstantsV2.RESULT_ERROR_PREFIX)) {
            Timber.e("waitStatus device error: $result")
            WaitResult.DeviceError(result)
        } else {
            null
        }
    }

    /** 定时运动完成状态，返回原始 [WaitResult]（含出错状态码），供诊断界面精确显示。 */
    suspend fun awaitMoveDuration(timeoutMs: Long = MOVE_TIMEOUT_MS): WaitResult =
        waitStatus(CtlConstantsV2.CMD_ACTION_MOVE_DURATION_STATUS_COMPLETED, timeoutMs)

    suspend fun waitMoveDurationStatusSuccess(timeoutMs: Long = MOVE_TIMEOUT_MS): Boolean =
        awaitMoveDuration(timeoutMs) == WaitResult.Completed

    /**
     * 移动到传感器
     * velocity:
     * ssId：0-向内 1-向外
     */
    fun moveToSs(motorId: Int, velocity: Int, duration: Int, ssId: Int): CtlSerialMessageV2 =
        command(
            CtlConstantsV2.CMD_ACTION_MOVE_TO_SS,
            byteArrayOf(motorId.toByte()) + velocity.toLeBytes() + duration.toLeBytes() +
                byteArrayOf(ssId.toByte())
        )

    fun moveOut(): CtlSerialMessageV2 =
        moveToSs(MOTOR_H, MOVE_OUT_VELOCITY, MOVE_OUT_DURATION, SS_OUT)

    fun moveIn(): CtlSerialMessageV2 =
        moveToSs(MOTOR_H, MOVE_IN_VELOCITY, MOVE_IN_DURATION, SS_IN)

    fun closeDoor(): CtlSerialMessageV2 =
        moveDuration(MOTOR_H, CLOSE_DOOR_VELOCITY, CLOSE_DOOR_DURATION)

    fun moveIn2mm(): CtlSerialMessageV2 =
        moveDuration(MOTOR_H, MOVE_IN_2MM_VELOCITY, MOVE_IN_2MM_DURATION)

    fun moveUp(): CtlSerialMessageV2 =
        moveDuration(MOTOR_V, MOVE_UP_VELOCITY, MOVE_UP_DURATION)

    fun moveDown(): CtlSerialMessageV2 =
        moveToSs(MOTOR_V, MOVE_DOWN_VELOCITY, MOVE_DOWN_DURATION, SS_IN)

    fun moveOutALittle(): CtlSerialMessageV2 =
        moveDuration(MOTOR_H, MOVE_OUT_LITTLE_VELOCITY, MOVE_OUT_LITTLE_DURATION)

    fun moveInALittle(): CtlSerialMessageV2 =
        moveDuration(MOTOR_H, MOVE_IN_LITTLE_VELOCITY, MOVE_IN_LITTLE_DURATION)

    /**
     * 吸液后芯片移入：抬起 -> 向内 -> 下压 -> home，每步轮询等待完成
     * @return 空字符串表示成功，否则为出错步骤及错误码
     */
    suspend fun moveChipInAfterAbsorb(): String {
        val steps = listOf(
            "moveUp" to moveUp(),
            "moveOutALittle" to moveOutALittle(),
            "moveInALittle" to moveInALittle(), // 可能会卡住，先松一下顶针
            "moveDown" to moveDown(),
            "moveIn" to moveIn()
        )

        for ((name, cmd) in steps) {
            val sendResult = readAllData(cmd)
            Timber.w("$name result: $sendResult")

            val waitResult = waitStatus(
                CtlConstantsV2.CMD_ACTION_MOVE_DURATION_STATUS_COMPLETED,
                MOVE_TIMEOUT_MS
            )
            when (waitResult) {
                is WaitResult.Completed -> Unit
                is WaitResult.DeviceError -> {
                    Timber.e("$name errorCode: ${waitResult.raw}")
                    return "$name: ${waitResult.raw}"
                }
                is WaitResult.Timeout -> {
                    Timber.e("$name timeout")
                    return "$name: timeout"
                }
            }
        }
        return ""
    }

    /** 移动到传感器完成状态，返回原始 [WaitResult]（含出错状态码），供诊断界面精确显示。 */
    suspend fun awaitMoveToSs(timeoutMs: Long = MOVE_TIMEOUT_MS): WaitResult =
        waitStatus(CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED, timeoutMs)

    suspend fun waitMoveToSsStatusSuccess(timeoutMs: Long = MOVE_TIMEOUT_MS): Boolean =
        awaitMoveToSs(timeoutMs) == WaitResult.Completed

    // ---------------------------------------------------------------------------------------
    // 机械动作重试封装：任一动作失败时先归零到已知位置再重试（见 MechanicalRetry），
    // 直接重发可能把卡在半途的滑台顶到硬限位而卡死。全部重试用尽后返回 false，
    // 由调用方（WorkMainViewModel）负责退卡并提示重试。
    // ---------------------------------------------------------------------------------------

    /** 取消当前动作并归零到已知位置。尽力而为：归零失败也不抛出，交由上层决定。 */
    suspend fun recoverHome() {
        readAllData(cancel())
        readAllData(homing())
        waitHomingStatusSuccess()
    }

    /** 发送 [cmd] 后按 [awaitCompletion] 轮询等待完成，带归零重试。完成返回 true。 */
    private suspend fun withRetry(
        name: String,
        send: () -> Unit,
        awaitCompletion: suspend () -> Boolean,
    ): Boolean = MechanicalRetry.run(
        name = name,
        recover = { recoverHome() },
    ) {
        send()
        awaitCompletion()
    }

    /** moveToSs 类动作（moveIn/moveOut 等）带归零重试。 */
    suspend fun moveToSsWithRetry(name: String, cmd: CtlSerialMessageV2): Boolean =
        withRetry(name, send = { readAllData(cmd) }, awaitCompletion = { waitMoveToSsStatusSuccess() })

    /** moveDuration 类动作（closeDoor/moveIn2mm 等）带归零重试。 */
    suspend fun moveDurationWithRetry(name: String, cmd: CtlSerialMessageV2): Boolean =
        withRetry(name, send = { readAllData(cmd) }, awaitCompletion = { waitMoveDurationStatusSuccess() })

    /** 归零动作带重试；归零本身即回到已知位，重试前仅取消当前动作，不再递归归零。 */
    suspend fun homingWithRetry(onProgress: (progressVal: Int) -> Unit = {}): Boolean =
        MechanicalRetry.run(name = "homing", recover = { readAllData(cancel()) }) {
            readAllData(homing())
            waitHomingStatusSuccess(onProgress = onProgress)
        }

    suspend fun moveInWithRetry(): Boolean = moveToSsWithRetry("moveIn", moveIn())

    suspend fun moveOutWithRetry(): Boolean = moveToSsWithRetry("moveOut", moveOut())

    suspend fun closeDoorWithRetry(): Boolean = moveDurationWithRetry("closeDoor", closeDoor())

    /** 激光扫描带归零重试。 */
    suspend fun scanWithRetry(cmd: CtlSerialMessageV2): Boolean =
        withRetry("scan", send = { readAllData(cmd) }, awaitCompletion = { waitScanStatusSuccess() })

    /** 吸液带归零重试；发送后先等待 [settleMs] 再轮询完成状态。 */
    suspend fun absorbWithRetry(cmd: CtlSerialMessageV2, settleMs: Long): Boolean =
        withRetry("absorb", send = { readAllData(cmd) }) {
            delay(settleMs)
            waitAbsorbStatusSuccess()
        }

    /** 吸液后多步移入序列（moveChipInAfterAbsorb）带归零重试；空错误串代表成功。 */
    suspend fun moveChipInAfterAbsorbWithRetry(): Boolean =
        MechanicalRetry.run(name = "moveChipInAfterAbsorb", recover = { recoverHome() }) {
            moveChipInAfterAbsorb().isEmpty()
        }

    /**
     * 退出试剂卡到取卡位。用于机械动作不可恢复失败后，让用户能取回卡片重试。
     * 尽力而为——本身也是机械动作，失败仅记录日志，不再抛出或递归退卡。
     */
    suspend fun ejectCard() {
        readAllData(cancel())
        readAllData(moveOut())
        waitMoveToSsStatusSuccess()
        readAllData(closeDoor())
        waitMoveDurationStatusSuccess()
    }

    /**
     * 吸水操作
     */
    fun absorb(milliseconds: Int): CtlSerialMessageV2 =
        command(CtlConstantsV2.CMD_ACTION_ABSORB, milliseconds.toLeBytes())

    /** 吸液完成状态，返回原始 [WaitResult]（含出错状态码），供诊断界面精确显示。 */
    suspend fun awaitAbsorb(timeoutMs: Long = ABSORB_TIMEOUT_MS): WaitResult =
        waitStatus(CtlConstantsV2.CMD_ACTION_ABSORB_STATUS_COMPLETED, timeoutMs)

    suspend fun waitAbsorbStatusSuccess(timeoutMs: Long = ABSORB_TIMEOUT_MS): Boolean =
        awaitAbsorb(timeoutMs) == WaitResult.Completed

    /**
     * 获取激光功率
     */
    fun getLDPwr(): CtlSerialMessageV2 = command(CtlConstantsV2.CMD_LD_PWR_OFFSET)

    /**
     * 设置激光功率
     */
    fun setLDPwr(offset: Int): CtlSerialMessageV2 =
        command(CtlConstantsV2.CMD_LD_PWR_OFFSET, byteArrayOf(offset.toByte()))

    /**
     * 打开/关闭激光
     */
    fun powerLD(onOff: Boolean): CtlSerialMessageV2 {
        val power: Byte = if (onOff) 0x01 else 0x00
        return command(
            CtlConstantsV2.CMD_GPIO_WRITE,
            byteArrayOf(CtlGpioConstV2.GPIO_LD_POWER, power)
        )
    }

    /**
     * 激光扫描
     */
    fun scan(velocity: Int, duration: Int): CtlSerialMessageV2 =
        command(
            CtlConstantsV2.CMD_ACTION_SCAN,
            velocity.toLeBytes() + duration.toLeBytes()
        )

    /** 激光扫描完成状态，返回原始 [WaitResult]（含出错状态码），供诊断界面精确显示。 */
    suspend fun awaitScan(timeoutMs: Long = SCAN_TIMEOUT_MS): WaitResult =
        waitStatus(CtlConstantsV2.CMD_ACTION_SCAN_STATUS_COMPLETED, timeoutMs)

    suspend fun waitScanStatusSuccess(timeoutMs: Long = SCAN_TIMEOUT_MS): Boolean =
        awaitScan(timeoutMs) == WaitResult.Completed

    /**
     * 查询扫描数据
     */
    fun queryData(): CtlSerialMessageV2 =
        command(CtlConstantsV2.CMD_ACTION_QUERY_DATA).also { Timber.w("queryData: $it") }

    /**
     * QR码扫描
     */
    fun readQR(): CtlSerialMessageV2 = command(CtlConstantsV2.CMD_ACTION_READ_QR)

    /**
     * 轮询等待QR码扫描结果；delay 可被取消，取消后轮询立即停止
     * @param keepWaiting 返回 false 时提前停止轮询（如用户取消）
     * @return QR码内容；出错或超时返回 CMD_ACTION_READ_QR_RESULT_NULL；keepWaiting 为 false 停止时返回空字符串
     */
    suspend fun waitReadQrResult(
        timeoutMs: Long = READ_QR_TIMEOUT_MS,
        keepWaiting: () -> Boolean = { true },
    ): String = pollLoop(
        tag = "waitReadQrResult",
        timeoutMs = timeoutMs,
        keepWaiting = keepWaiting,
        onStop = { "" },
        onTimeout = {
            Timber.e("waitReadQrResult timeout ${timeoutMs}ms")
            CtlConstantsV2.CMD_ACTION_READ_QR_RESULT_NULL
        },
    ) { result ->
        if (!isSuccess(result)) {
            null
        } else if (result.contains(CtlConstantsV2.CMD_ACTION_READ_QR_STATUS_COMPLETED)) {
            parseQrData(result).ifEmpty { null }
        } else if (result.contains(CtlConstantsV2.CMD_ACTION_READ_QR_STATUS_ERROR) &&
            result.contains("ERROR-QR")
        ) {
            CtlConstantsV2.CMD_ACTION_READ_QR_RESULT_NULL
        } else {
            null
        }
    }

    /**
     * 一次串口收发事务：清缓冲 -> 发送 -> 读应答 -> 解析出 byteData。
     * drain+send+read 整体加锁保证原子。无应答或解析失败返回 null（分别记日志区分）。
     */
    private fun transceive(hexMsg: String, timeoutMs: Int): ByteArray? {
        val buffer = synchronized(serialLock) {
            val serialHelper = App.getSerialHelperOrNull()
            if (serialHelper == null) {
                // board power guard 拦截上电、串口还没 open() 时会走到这里
                // （例如锁定期间仍可进入的设备信息页发起握手）
                Timber.w("transceive skipped: serial port not open yet")
                return@synchronized null
            }
            serialHelper.drainInput()
            serialHelper.sendHex(hexMsg)
            serialHelper.readAllData(timeoutMs)
        }
        if (buffer == null) {
            Timber.w("transceive no response within %dms", timeoutMs)
            return null
        }

        val receiverBuf: ByteBuf = Unpooled.buffer(buffer.size)
        try {
            receiverBuf.writeBytes(buffer, 0, buffer.size)
            val bytes = CtlSerialMessageV2.fromByteBuf(receiverBuf)?.byteData
            if (bytes == null) {
                Timber.e("transceive parse failed, %d raw bytes", buffer.size)
            }
            return bytes
        } finally {
            // 确保释放资源
            receiverBuf.release()
        }
    }

    fun readAllData(cmd: CtlSerialMessageV2, timeoutMs: Int = READ_TIMEOUT_MS): String {
        val hexMsg = cmd.toHexString()
        Timber.w("readAllData msg: %s", hexMsg)
        return transceive(hexMsg, timeoutMs)?.toString(Charsets.UTF_8) ?: ""
    }

    fun readAllDataByteArray(cmd: CtlSerialMessageV2, timeoutMs: Int = READ_TIMEOUT_MS): ByteArray? {
        val hexMsg = cmd.toHexString()
        val maxRetries = 3
        var lastError: Throwable? = null

        for (attempt in 1..maxRetries) {
            Timber.w("readAllDataByteArray attempt=%d/%d msg=%s", attempt, maxRetries, hexMsg)
            try {
                val data = transceive(hexMsg, timeoutMs)
                if (data != null &&
                    data.toString(Charsets.UTF_8).startsWith(CtlConstantsV2.RESULT_HAS_DATA_PREFIX)
                ) {
                    if (attempt > 1) {
                        Timber.i("readAllDataByteArray succeeded on attempt=%d/%d", attempt, maxRetries)
                    }
                    return data.copyOfRange(2, data.size)
                }
            } catch (t: Throwable) {
                lastError = t
                Timber.e(t, "readAllDataByteArray error attempt=%d/%d sendMsgHex=%s", attempt, maxRetries, hexMsg)
                if (attempt < maxRetries) {
                    Thread.sleep(200)
                }
            }
        }

        lastError?.let { throw it }
        return null
    }

    /** 构造一条指令：设置 cmd、可选 byteData，并附带带 SID 的 paramData。 */
    private fun command(cmd: Byte, byteData: ByteArray? = null): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = cmd
        if (byteData != null) message.byteData = byteData
        message.paramData = getParamData()
        return message
    }

    /** Int 转小端 4 字节，串口协议统一用小端。 */
    private fun Int.toLeBytes(): ByteArray = byteArrayOf(
        (this and 0xFF).toByte(),
        ((this shr 8) and 0xFF).toByte(),
        ((this shr 16) and 0xFF).toByte(),
        ((this shr 24) and 0xFF).toByte(),
    )

    private fun getParamData(): SerialQueryParams {
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(
            CtlConstantsV2.PARAM_SID,
            SocketSidUtils.nextControlSid().toString()
        )
        return paramData
    }

    private fun isSuccess(result: String): Boolean {
        return result.startsWith(CtlConstantsV2.RESULT_SUCCESS_PREFIX)
    }

    /**
     * 从成功应答里取 QR 内容：结构为 ...COMPLETED-QR:<data>,... 。
     * 帧不含标记或结构不符时返回空串。
     */
    private fun parseQrData(result: String): String {
        if (!result.contains(QR_COMPLETED_TOKEN)) return ""
        return runCatching {
            result.split(QR_COMPLETED_TOKEN)[1].split(":")[1].split(",")[0]
        }.getOrDefault("")
    }
}