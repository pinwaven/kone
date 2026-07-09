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
 * 指令生成帮助类
 */
object CtlCommandsV2 {
    var homingProgressVal = 10

    var isWaitScanStatusSuccessCancel = false

    var isWaitAbsorbStatusSuccessCancel = false

    val EMPTY = CtlSerialMessageV2()

    val delayMs: Long = 150

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

    fun processHomingStatus(customFunction: (progressVal: Int) -> Unit) {
        val cmd = poll()
        val result = this.readAllData(cmd)

        if (result.isNotEmpty()) {
            println("processHomingStatus result: $result")

            var resultStatus = ""
            if (isSuccess(result)) {
                for (key in CtlConstantsV2.HOMING_STATUS_RESULT_MAP.keys) {
                    if (result.contains("m:$key")) {
                        resultStatus = CtlConstantsV2.HOMING_STATUS_RESULT_MAP[key]!!
                        break
                    }
                }

                for (key in CtlConstantsV2.HOMING_STATUS_MAP.keys) {
                    if (result.contains("s:$key")) {
                        homingProgressVal = CtlConstantsV2.HOMING_STATUS_MAP[key]!!
                        Timber.w("status: $key statusVal: $resultStatus")
                        break
                    }
                }
            }
        }

        Thread.sleep(delayMs)
        customFunction(homingProgressVal)
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

    suspend fun waitMoveDurationStatusSuccess(): Boolean {
        while (true) {
            val result = this.readAllData(poll())

            if (result.isNotEmpty()) {
                println("waitMoveDurationStatusSuccess result: $result")

                if (isSuccess(result)) {
                    if (result.contains(CtlConstantsV2.CMD_ACTION_MOVE_DURATION_STATUS_COMPLETED)) {
                        return true
                    }
                }
            }

            // delay 可被取消：取消指令后轮询立即停止
            delay(delayMs)
        }
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
     * 轮询等待动作完成或出错
     * @return 空字符串表示成功，否则为错误码
     */
    private fun waitMoveStatusOrError(): String {
        while (true) {
            val result = readAllData(poll())

            if (result.isNotEmpty()) {
                println("waitMoveStatusOrError result: $result")

                if (isSuccess(result)) {
                    if (result.contains(CtlConstantsV2.CMD_ACTION_MOVE_DURATION_STATUS_COMPLETED)) {
                        return ""
                    }
                    if (result.contains(CtlConstantsV2.CMD_ACTION_STATUS_ERROR)) {
                        return result
                    }
                } else if (result.startsWith(CtlConstantsV2.RESULT_ERROR_PREFIX)) {
                    return result
                }
            }

            Thread.sleep(delayMs)
        }
    }

    /**
     * 吸液后芯片移入：抬起 -> 向内 -> 下压 -> home，每步轮询等待完成
     * @return 空字符串表示成功，否则为出错步骤及错误码
     */
    fun moveChipInAfterAbsorb(): String {
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

            val errorCode = waitMoveStatusOrError()
            if (errorCode.isNotEmpty()) {
                Timber.e("$name errorCode: $errorCode")
                return "$name: $errorCode"
            }
        }
        return ""
    }

    suspend fun waitMoveToSsStatusSuccess(): Boolean {
        while (true) {
            val result = this.readAllData(poll())

            if (result.isNotEmpty()) {
                println("waitMoveToSsStatusSuccess result: $result")

                if (isSuccess(result)) {
                    if (result.contains(CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED)) {
                        return true
                    }
                }
            }

            // delay 可被取消：取消指令后轮询立即停止
            delay(delayMs)
        }
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

    fun waitAbsorbStatusSuccess(): Boolean {
        val cmd = poll()
        val result = this.readAllData(cmd)

        if (result.isNotEmpty()) {
            println("waitAbsorbStatusSuccess result: $result")

            if (isSuccess(result)) {
                if (result.contains(CtlConstantsV2.CMD_ACTION_ABSORB_STATUS_COMPLETED)) {
                    return true
                }
            }
        }

        Thread.sleep(delayMs)

        if (!isWaitAbsorbStatusSuccessCancel) {
            return waitAbsorbStatusSuccess()
        } else {
            val cancelResult = readAllData(cancel())
            Timber.w("cancelResult: $cancelResult")
            return false
        }
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

    fun waitScanStatusSuccess(): Boolean {
        val cmd = poll()
        val result = this.readAllData(cmd)

        if (result.isNotEmpty()) {
            println("waitScanStatusSuccess result: $result")

            if (isSuccess(result)) {
                if (result.contains(CtlConstantsV2.CMD_ACTION_SCAN_STATUS_COMPLETED)) {
                    return true
                }
            }
        }

        Thread.sleep(delayMs)

        if (!isWaitScanStatusSuccessCancel) {
            return waitScanStatusSuccess()
        } else {
            val cancelResult = readAllData(cancel())
            Timber.w("cancelResult: $cancelResult")
            return false
        }
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

    fun processReadQRStatus(customFunction: (qrCodeData: String) -> Unit) {
        val cmd = poll()
        val result = this.readAllData(cmd)

        var qrCodeData = ""
        if (result.isNotEmpty()) {
            println("processReadQRStatus result: $result")

            if (isSuccess(result)) {
                if (result.contains(CtlConstantsV2.CMD_ACTION_READ_QR_STATUS_COMPLETED)) {
                    val successKey = "COMPLETED-QR"
                    if (result.contains(successKey)) {
                        val results = result.split(successKey)
                        val qrCodeDataTmp = results[1].split(":")[1]
                        qrCodeData = qrCodeDataTmp.split(",")[0]
                    }
                } else if (result.contains(CtlConstantsV2.CMD_ACTION_READ_QR_STATUS_ERROR)) {
                    val successKey = "ERROR-QR"
                    if (result.contains(successKey)) {
                        qrCodeData = CtlConstantsV2.CMD_ACTION_READ_QR_RESULT_NULL
                    }
                }
            }
        }

        Thread.sleep(delayMs)
        customFunction(qrCodeData)
    }

    fun readAllData(cmd: CtlSerialMessageV2): String {
        val hexMsg = cmd.toHexString()
        Timber.w("readAllData msg: %s", hexMsg)

        App.getSerialHelper().sendHex(hexMsg)

        val buffer = App.getSerialHelper().readAllData()
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

    fun readAllDataByteArray(cmd: CtlSerialMessageV2): ByteArray? {
        val hexMsg = cmd.toHexString()
        val maxRetries = 3
        var lastError: Throwable? = null

        for (attempt in 1..maxRetries) {
            Timber.w("readAllDataByteArray attempt=%d/%d msg=%s", attempt, maxRetries, hexMsg)
            try {
                App.getSerialHelper().sendHex(hexMsg)
                val buffer = App.getSerialHelper().readAllData()
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