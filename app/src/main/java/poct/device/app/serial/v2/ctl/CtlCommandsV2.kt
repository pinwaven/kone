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

    val delayMs: Long = 150

    // 各类动作的轮询超时时间：超时后停止轮询并发送取消指令，避免卡死后续指令
    const val MOVE_TIMEOUT_MS = 30_000L
    const val ABSORB_TIMEOUT_MS = 300_000L
    const val SCAN_TIMEOUT_MS = 60_000L
    const val HOMING_TIMEOUT_MS = 60_000L
    const val READ_QR_TIMEOUT_MS = 30_000L

    // 单次读取串口应答的默认超时（毫秒）。慢指令可传入更大的值。
    const val READ_TIMEOUT_MS = 1000

    /**
     * 系统状态轮询
     */
    fun poll(): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_POLL
        message.paramData = getParamData()
        return message
    }

    /**
     * 心跳
     */
    fun hi(): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_HI
        message.paramData = getParamData()
        return message
    }

    /**
     * 读取GPIO状态
     */
    fun gpioRead(): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_GPIO_READ
        message.paramData = getParamData()
        return message
    }

    /**
     * 读取GPIO状态（是否存在卡片）
     */
    fun gpioReadHasCard(result: String): Boolean {
        return result.contains("ss_card:0")
    }

    /**
     * 取消当前动作
     */
    fun cancel(): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_ACTION_CANCEL
        message.paramData = getParamData()
        return message
    }

    /**
     * 归零操作
     */
    fun homing(): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_ACTION_HOMING
        message.paramData = getParamData()
        return message
    }

    /**
     * 轮询等待归零完成；delay 可被取消，取消后轮询立即停止
     * @param onProgress 进度回调（HOMING_STATUS_MAP 中的进度值）
     * @return true 表示归零完成，false 表示出错或超时
     */
    suspend fun waitHomingStatusSuccess(
        timeoutMs: Long = HOMING_TIMEOUT_MS,
        onProgress: (progressVal: Int) -> Unit = {},
    ): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime <= timeoutMs) {
            val result = this.readAllData(poll())

            if (result.isNotEmpty()) {
                println("waitHomingStatusSuccess result: $result")

                if (isSuccess(result)) {
                    for (key in CtlConstantsV2.HOMING_STATUS_MAP.keys) {
                        if (result.contains("s:$key")) {
                            val progressVal = CtlConstantsV2.HOMING_STATUS_MAP[key]!!
                            Timber.w("homing status: $key progressVal: $progressVal")
                            onProgress(progressVal)
                            if (progressVal >= CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED) {
                                return true
                            }
                            break
                        }
                    }
                } else if (result.startsWith(CtlConstantsV2.RESULT_ERROR_PREFIX)) {
                    Timber.e("waitHomingStatusSuccess device error: $result")
                    return false
                }
            }

            delay(delayMs)
        }

        Timber.e("waitHomingStatusSuccess timeout ${timeoutMs}ms, sending cancel")
        readAllData(cancel())
        return false
    }

    /**
     * 定时运动
     */
    fun moveDuration(motorId: Int, velocity: Int, duration: Int): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_ACTION_MOVE_DURATION
        message.byteData =
            byteArrayOf(
                motorId.toByte(),
                (velocity and 0xFF).toByte(),
                ((velocity shr 8) and 0xFF).toByte(),
                ((velocity shr 16) and 0xFF).toByte(),
                ((velocity shr 24) and 0xFF).toByte(),
                (duration and 0xFF).toByte(),
                ((duration shr 8) and 0xFF).toByte(),
                ((duration shr 16) and 0xFF).toByte(),
                ((duration shr 24) and 0xFF).toByte()
            )
        message.paramData = getParamData()
        return message
    }

    /**
     * 通用轮询等待：完成、设备出错或超时必定返回，不会永久阻塞后续指令。
     * delay 可被取消：取消指令后轮询立即停止；超时后发送取消指令，避免设备停留在动作中。
     */
    private suspend fun waitStatus(completedToken: String, timeoutMs: Long): WaitResult {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime <= timeoutMs) {
            val result = readAllData(poll())

            if (result.isNotEmpty()) {
                println("waitStatus result: $result")

                if (isSuccess(result)) {
                    if (result.contains(completedToken)) {
                        return WaitResult.Completed
                    }
                    if (result.contains(CtlConstantsV2.CMD_ACTION_STATUS_ERROR)) {
                        Timber.e("waitStatus device error: $result")
                        return WaitResult.DeviceError(result)
                    }
                } else if (result.startsWith(CtlConstantsV2.RESULT_ERROR_PREFIX)) {
                    Timber.e("waitStatus device error: $result")
                    return WaitResult.DeviceError(result)
                }
            }

            delay(delayMs)
        }

        Timber.e("waitStatus timeout ${timeoutMs}ms waiting $completedToken, sending cancel")
        readAllData(cancel())
        return WaitResult.Timeout
    }

    suspend fun waitMoveDurationStatusSuccess(timeoutMs: Long = MOVE_TIMEOUT_MS): Boolean {
        return waitStatus(
            CtlConstantsV2.CMD_ACTION_MOVE_DURATION_STATUS_COMPLETED,
            timeoutMs
        ) == WaitResult.Completed
    }

    /**
     * 移动到传感器
     * velocity:
     * ssId：0-向内 1-向外
     */
    fun moveToSs(motorId: Int, velocity: Int, duration: Int, ssId: Int): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_ACTION_MOVE_TO_SS
        message.byteData =
            byteArrayOf(
                motorId.toByte(),
                (velocity and 0xFF).toByte(),
                ((velocity shr 8) and 0xFF).toByte(),
                ((velocity shr 16) and 0xFF).toByte(),
                ((velocity shr 24) and 0xFF).toByte(),
                (duration and 0xFF).toByte(),
                ((duration shr 8) and 0xFF).toByte(),
                ((duration shr 16) and 0xFF).toByte(),
                ((duration shr 24) and 0xFF).toByte(),
                ssId.toByte()
            )
        message.paramData = getParamData()
        return message
    }

    fun moveOut(): CtlSerialMessageV2 {
        return moveToSs(0, -70000, 11000, 1)
    }

    fun moveIn(): CtlSerialMessageV2 {
        return moveToSs(0, 70000, 11000, 0)
    }

    fun closeDoor(): CtlSerialMessageV2 {
        return moveDuration(0, 70000, 1000)
    }

    fun moveIn2mm(): CtlSerialMessageV2 {
        return moveDuration(0, 70000, 250)
    }

    fun moveUp(): CtlSerialMessageV2 {
        return moveDuration(1, 60000, 1000)
    }

    fun moveDown(): CtlSerialMessageV2 {
        return moveToSs(1, -50000, 2500, 0)
    }

    fun moveOutALittle(): CtlSerialMessageV2 {
        return moveDuration(0, -50000, 400)
    }

    fun moveInALittle(): CtlSerialMessageV2 {
        return moveDuration(0, 50000, 100)
    }

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

    suspend fun waitMoveToSsStatusSuccess(timeoutMs: Long = MOVE_TIMEOUT_MS): Boolean {
        return waitStatus(
            CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED,
            timeoutMs
        ) == WaitResult.Completed
    }

    /**
     * 吸水操作
     */
    fun absorb(milliseconds: Int): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_ACTION_ABSORB
        message.byteData =
            byteArrayOf(
                (milliseconds and 0xFF).toByte(),
                ((milliseconds shr 8) and 0xFF).toByte(),
                ((milliseconds shr 16) and 0xFF).toByte(),
                ((milliseconds shr 24) and 0xFF).toByte()
            )
        message.paramData = getParamData()
        return message
    }

    suspend fun waitAbsorbStatusSuccess(timeoutMs: Long = ABSORB_TIMEOUT_MS): Boolean {
        return waitStatus(
            CtlConstantsV2.CMD_ACTION_ABSORB_STATUS_COMPLETED,
            timeoutMs
        ) == WaitResult.Completed
    }

    /**
     * 获取激光功率
     */
    fun getLDPwr(): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_LD_PWR_OFFSET
        message.paramData = getParamData()
        return message
    }

    /**
     * 设置激光功率
     */
    fun setLDPwr(offset: Int): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_LD_PWR_OFFSET
        message.byteData = byteArrayOf(offset.toByte())
        message.paramData = getParamData()
        return message
    }

    /**
     * 打开/关闭激光
     */
    fun powerLD(onOff: Boolean): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_GPIO_WRITE
        val power: Byte = if (onOff) 0x01 else 0x00
        message.byteData = byteArrayOf(CtlGpioConstV2.GPIO_LD_POWER, power)
        message.paramData = getParamData()
        return message
    }

    /**
     * 激光扫描
     */
    fun scan(velocity: Int, duration: Int): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_ACTION_SCAN
        message.byteData =
            byteArrayOf(
                (velocity and 0xFF).toByte(),
                ((velocity shr 8) and 0xFF).toByte(),
                ((velocity shr 16) and 0xFF).toByte(),
                ((velocity shr 24) and 0xFF).toByte(),
                (duration and 0xFF).toByte(),
                ((duration shr 8) and 0xFF).toByte(),
                ((duration shr 16) and 0xFF).toByte(),
                ((duration shr 24) and 0xFF).toByte()
            )
        message.paramData = getParamData()
        return message
    }

    suspend fun waitScanStatusSuccess(timeoutMs: Long = SCAN_TIMEOUT_MS): Boolean {
        return waitStatus(
            CtlConstantsV2.CMD_ACTION_SCAN_STATUS_COMPLETED,
            timeoutMs
        ) == WaitResult.Completed
    }

    /**
     * 查询扫描数据
     */
    fun queryData(): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_ACTION_QUERY_DATA
        message.paramData = getParamData()
        Timber.w("queryData: $message")
        return message
    }

    /**
     * QR码扫描
     */
    fun readQR(): CtlSerialMessageV2 {
        val message = CtlSerialMessageV2()
        message.cmd = CtlConstantsV2.CMD_ACTION_READ_QR
        message.paramData = getParamData()
        return message
    }

    /**
     * 轮询等待QR码扫描结果；delay 可被取消，取消后轮询立即停止
     * @param keepWaiting 返回 false 时提前停止轮询（如用户取消）
     * @return QR码内容；出错或超时返回 CMD_ACTION_READ_QR_RESULT_NULL；keepWaiting 为 false 停止时返回空字符串
     */
    suspend fun waitReadQrResult(
        timeoutMs: Long = READ_QR_TIMEOUT_MS,
        keepWaiting: () -> Boolean = { true },
    ): String {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime <= timeoutMs) {
            if (!keepWaiting()) {
                return ""
            }

            val result = this.readAllData(poll())

            if (result.isNotEmpty()) {
                println("waitReadQrResult result: $result")

                if (isSuccess(result)) {
                    if (result.contains(CtlConstantsV2.CMD_ACTION_READ_QR_STATUS_COMPLETED)) {
                        val successKey = "COMPLETED-QR"
                        if (result.contains(successKey)) {
                            val qrCodeData = runCatching {
                                result.split(successKey)[1].split(":")[1].split(",")[0]
                            }.getOrDefault("")
                            if (qrCodeData.isNotEmpty()) {
                                return qrCodeData
                            }
                        }
                    } else if (result.contains(CtlConstantsV2.CMD_ACTION_READ_QR_STATUS_ERROR)) {
                        if (result.contains("ERROR-QR")) {
                            return CtlConstantsV2.CMD_ACTION_READ_QR_RESULT_NULL
                        }
                    }
                }
            }

            delay(delayMs)
        }

        Timber.e("waitReadQrResult timeout ${timeoutMs}ms")
        return CtlConstantsV2.CMD_ACTION_READ_QR_RESULT_NULL
    }

    fun readAllData(cmd: CtlSerialMessageV2, timeoutMs: Int = READ_TIMEOUT_MS): String {
        val hexMsg = cmd.toHexString()
        Timber.w("readAllData msg: %s", hexMsg)

        // 发送前清空接收缓冲区，避免读到上一条指令的迟到应答
        App.getSerialHelper().drainInput()
        App.getSerialHelper().sendHex(hexMsg)

        val buffer = App.getSerialHelper().readAllData(timeoutMs)
        if (buffer != null) {
            val receiverBuf: ByteBuf = Unpooled.buffer(buffer.size)
            try {
                receiverBuf.writeBytes(buffer, 0, buffer.size)
                val resultMsg = CtlSerialMessageV2.fromByteBuf(receiverBuf)
                if (resultMsg != null) {
                    return resultMsg.byteData!!.toString(Charsets.UTF_8)
                }
            } finally {
                // 确保释放资源
                receiverBuf.release()
            }
        }
        return ""
    }

    fun readAllDataByteArray(cmd: CtlSerialMessageV2, timeoutMs: Int = READ_TIMEOUT_MS): ByteArray? {
        val hexMsg = cmd.toHexString()
        val maxRetries = 3
        var lastError: Throwable? = null

        for (attempt in 1..maxRetries) {
            Timber.w("readAllDataByteArray attempt=%d/%d msg=%s", attempt, maxRetries, hexMsg)
            try {
                // 每次发送前清空接收缓冲区，避免读到上一条指令的迟到应答
                App.getSerialHelper().drainInput()
                App.getSerialHelper().sendHex(hexMsg)
                val buffer = App.getSerialHelper().readAllData(timeoutMs)
                if (buffer != null) {
                    val receiverBuf: ByteBuf = Unpooled.buffer(buffer.size)
                    try {
                        receiverBuf.writeBytes(buffer, 0, buffer.size)
                        val resultMsg = CtlSerialMessageV2.fromByteBuf(receiverBuf)
                        if (resultMsg != null) {
                            val byteDataString = resultMsg.byteData!!.toString(Charsets.UTF_8)
                            if (byteDataString.startsWith(CtlConstantsV2.RESULT_HAS_DATA_PREFIX)) {
                                if (attempt > 1) {
                                    Timber.i("readAllDataByteArray succeeded on attempt=%d/%d", attempt, maxRetries)
                                }
                                return resultMsg.byteData!!.copyOfRange(2, resultMsg.byteData!!.size)
                            }
                        }
                    } finally {
                        receiverBuf.release()
                    }
                } else {
                    Timber.w("readAllDataByteArray buffer null attempt=%d/%d", attempt, maxRetries)
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
}