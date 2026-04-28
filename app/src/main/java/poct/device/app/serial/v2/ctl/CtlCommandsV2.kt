package poct.device.app.serial.v2.ctl

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import poct.device.app.App
import poct.device.app.serial.SerialQueryParams
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

    fun waitMoveDurationStatusSuccess(): Boolean {
        val cmd = poll()
        val result = this.readAllData(cmd)

        if (result.isNotEmpty()) {
            println("waitMoveDurationStatusSuccess result: $result")

            if (isSuccess(result)) {
                if (result.contains(CtlConstantsV2.CMD_ACTION_MOVE_DURATION_STATUS_COMPLETED)) {
                    return true
                }
            }
        }

        Thread.sleep(delayMs)
        return waitMoveDurationStatusSuccess()
    }

    /**
     * 移动到传感器
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

    fun waitMoveToSsStatusSuccess(): Boolean {
        val cmd = poll()
        val result = this.readAllData(cmd)

        if (result.isNotEmpty()) {
            println("waitMoveToSsStatusSuccess result: $result")

            if (isSuccess(result)) {
                if (result.contains(CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED)) {
                    return true
                }
            }
        }

        Thread.sleep(delayMs)
        return waitMoveToSsStatusSuccess()
    }

    /**
     * 吸收操作
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
        Timber.w("readAllData msg: %s", hexMsg)

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
                        return resultMsg.byteData!!.copyOfRange(2, resultMsg.byteData!!.size)
                    }
                }
            } finally {
                // 确保释放资源
                receiverBuf.release()
            }
        }
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