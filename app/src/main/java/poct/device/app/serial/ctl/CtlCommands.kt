package poct.device.app.serial.ctl

import info.szyh.socket.utils.SocketSidUtils
import okhttp3.internal.toHexString
import poct.device.app.bean.ConfigAdjustBean
import poct.device.app.serial.SerialQueryParams
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Base64
import java.util.zip.CRC32


/**
 * 指令生成帮助类
 */
object CtlCommands {
    val EMPTY = CtlSerialMessage()

    /**
     * 开始检测，包含片仓复位
     */
    fun handleShake(): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_HANDSHAKE
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 读取信息
     */
    fun readSys(): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_READ_SYS
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 开始检测，包含片仓复位
     * DELAY: Step= 1:移入片仓中 2:打开吸水阀中 3:关闭吸水阀中 4:移动到检测位置中 5:检测中 6:移出片仓中
     */
    fun startCheck(): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_START_CHECK
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 结束检测，自动移动片仓
     */
    fun stopCheck(): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_STOP_CHECK
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 自检,Mode=1:快速自检老自检，2：标准模式，3：专家模式
     */
    fun selfCheck(mode: Int = 1): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_SELF_CHECK
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_MODE, mode.toString())
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 低功耗
     * 低功耗取值说明:
     * 0:正常工作，1:一般休眠,
     * 2:深度休眠，3:关机模式
     */
    fun lowPower(flag: Boolean = true): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_LOW_POWER
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_MODE, if (flag) "2" else "0")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 片仓移入
     * speed: 转速
     */
    fun moveIn(speed: Int = 600): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_MOVE_IN
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        paramData.addParam(CtlConstants.PARAM_SPEED, speed.toString())
        message.paramData = paramData
        return message
    }

    /**
     * 片仓移出
     *  speed: 转速
     */
    fun moveOut(speed: Int = 600): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_MOVE_OUT
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        paramData.addParam(CtlConstants.PARAM_SPEED, speed.toString())
        message.paramData = paramData
        return message
    }

    /**
     * 打开吸水阀，吸水完成后自动关闭
     * XsTime=60,Pos=5
     * XsTime=吸水时间(s),Pos=移动距离mm,
     */
    fun openXs(xsTime: Int = 60, pos: Int = 40): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_OPEN_XS
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        paramData.addParam(CtlConstants.PARAM_XS_TIME, xsTime.toString())
        paramData.addParam(CtlConstants.PARAM_POS, pos.toString())
        message.paramData = paramData
        return message
    }

//    /**
//     * 关闭吸水阀
//     */
//    fun closeXs(): CtlSerialMessage {
//        val message = CtlSerialMessage()
//        message.type = CtlConstants.TYPE_CONTROL
//        message.cmd = CtlConstants.CMD_CLOSE_XS
//        val paramData = SerialQueryParams("", false, ",")
//        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
//        message.paramData = paramData
//        return message
//    }


    /**
     * 扫描测试点
     * Start=,End=,PPMM=80,Speed=40|50|60,
     * Start=开始位置(mm),End=结束位置,PPMM=每毫米检测次数,Speed=移动速度,
     */
    fun scanTest(start: Int, end: Int, ppmm: Int = 80, speed: Int = 40): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_SCAN_TEST
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        paramData.addParam(CtlConstants.PARAM_START, start.toString())
        paramData.addParam(CtlConstants.PARAM_END, end.toString())
        paramData.addParam(CtlConstants.PARAM_PPMM, ppmm.toString())
        paramData.addParam(CtlConstants.PARAM_SPEED, speed.toString())
        message.paramData = paramData
        return message
    }


    /**
     * 扫码,成功时返回Data
     */
    fun scanCode(): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_SCAN_CODE
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 片仓复位
     */
    fun resetCase(mode: Int = 1): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_RESET_CASE
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_MODE, mode.toString())
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 数据读取， DELAY/SUCCESS： Amount，TrimIndex，Data
     */
    fun readData(): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_READ_DATA
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 设置信息
     */
    fun setSysInfo(id: String): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_SET_SYS
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_ID, id)
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 校准配置
     */
    fun adjustConfig(bean: ConfigAdjustBean): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_ADJUST_CONFIG
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_OUT_POS, bean.posName1)
        paramData.addParam(CtlConstants.PARAM_READ_POS, bean.posName2)
        paramData.addParam(CtlConstants.PARAM_CHECK_POS, bean.posName3)
        paramData.addParam(CtlConstants.PARAM_ENHANCE, bean.jcName1)
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 开始校准
     */
    fun startAdjust(): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_ADJUST_START
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 停止校准
     */
    fun stopAdjust(): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_ADJUST_STOP
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 测试
     */
    fun testCmdRequest(sid: Int, cmdLen: Int): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_TEST_REQUEST
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_TEST_SID, sid.toString())
        var cmdStr = ""
        for (i in 0..cmdLen) {
            cmdStr += "a"
        }
        paramData.addParam(CtlConstants.PARAM_TEST, cmdStr)
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }
    /**
     * 测试
     */
    fun testCmdRequest(): CtlSerialMessage {
        val message = CtlSerialMessage()
        message.type = CtlConstants.TYPE_CONTROL
        message.cmd = CtlConstants.CMD_TEST_RESPONSE
        val paramData = SerialQueryParams("", false, ",")
        paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
        message.paramData = paramData
        return message
    }

    /**
     * 进入升级功能
     * Mode=1为小板，Mode=0为大板
     * File文件目录
     */
    fun upgrade(mode: Int, file: String): List<CtlSerialMessage> {
        val sourceFile = File(file)
        if(!sourceFile.exists()) {
            return emptyList()
        }
        val msgList = ArrayList<CtlSerialMessage>()

        try {
            // 每次切片的文件大小 296B
            val partSize = 296
            // 文件大小
            val fileLength = sourceFile.length()
            val crcValue = calcFileCrcValue(sourceFile)
            // 切片后的小文件
            val chunks = splitFileIntoChunks(sourceFile, partSize)
            // 需要上传多少次
            var partCount = (fileLength / partSize).toInt()
            if ((fileLength % partSize).toInt() != 0) {
                partCount ++
            }
            /**************STEP1**************/
            val message1 = CtlSerialMessage()
            message1.type = CtlConstants.TYPE_CONTROL
            message1.cmd = CtlConstants.CMD_CTL_UPGRADE
            val paramData1 = SerialQueryParams("", false, ",")
            paramData1.addParam(CtlConstants.PARAM_MODE, mode.toString())
            // 1-准备上传, 2-正在传输, 3-传输结束
            paramData1.addParam(CtlConstants.PARAM_STEP, "1")
            // 总数
            paramData1.addParam(CtlConstants.PARAM_UPGRADE_TOTAL, partCount.toString())
            // 文件大小
            paramData1.addParam(CtlConstants.PARAM_UPGRADE_SIZE, fileLength.toString())
            // SID
            paramData1.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
            message1.paramData = paramData1
            msgList.add(message1)
            var index = 0
            /**************STEP2**************/
            for (chunk in chunks) {
                val chunkBytes = chunk.readBytes()
                val message = CtlSerialMessage()
                message.type = CtlConstants.TYPE_CONTROL
                message.cmd = CtlConstants.CMD_CTL_UPGRADE
                val paramData = SerialQueryParams("", false, ",")
                paramData.addParam(CtlConstants.PARAM_MODE, mode.toString())
                // 1-准备上传, 2-正在传输, 3-传输结束
                paramData.addParam(CtlConstants.PARAM_STEP, "2")
                // 传输数据
                paramData.addParam(CtlConstants.PARAM_DATA, Base64.getEncoder().encodeToString(chunkBytes))
                // 数据块序号
                paramData.addParam(CtlConstants.PARAM_UPGRADE_COUNT, (index + 1).toString())
                // 每个切片文件的crc
                val crcSubValue = calcFileCrcValue(chunk)
                paramData.addParam(CtlConstants.PARAM_UPGRADE_CRC, crcSubValue)
                // 总数
                paramData.addParam(CtlConstants.PARAM_UPGRADE_TOTAL, partCount.toString())
                // SID
                paramData.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
                message.paramData = paramData
                msgList.add(message)
                index ++
            }
            /**************STEP3**************/
            val message3 = CtlSerialMessage()
            message3.type = CtlConstants.TYPE_CONTROL
            message3.cmd = CtlConstants.CMD_CTL_UPGRADE
            val paramData3 = SerialQueryParams("", false, ",")
            paramData3.addParam(CtlConstants.PARAM_MODE, mode.toString())
            // 1-准备上传, 2-正在传输, 3-传输结束
            paramData3.addParam(CtlConstants.PARAM_STEP, "3")
            // 文件的crc
            paramData3.addParam(CtlConstants.PARAM_UPGRADE_CRC, crcValue)
            // SID
            paramData3.addParam(CtlConstants.PARAM_SID, SocketSidUtils.nextControlSid().toString())
            message3.paramData = paramData3
            msgList.add(message3)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            // 删除临时文件
            val tempDir = File(sourceFile.parentFile, "chunks")
            if (tempDir.exists()) {
                tempDir.delete()
            }
        }

        return msgList
    }

    private fun splitFileIntoChunks(file: File, chunkSize: Int): List<File> {
        val chunks = mutableListOf<File>()
        val inputStream = FileInputStream(file)
        var currentChunkSize = 0
        var chunkIndex = 0
        val buffer = ByteArray(chunkSize)

        val tempDir = File(file.parentFile, "chunks")
        tempDir.mkdir()

        var tempFile = File(tempDir, "chunk_${chunkIndex}")
        var outputStream = FileOutputStream(tempFile)

        while (true) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            outputStream.write(buffer, 0, read)
            currentChunkSize += read

            outputStream.close()
            chunks.add(tempFile)
            chunkIndex++
            currentChunkSize = 0
            // 创建新文件
            tempFile = File(tempDir, "chunk_$chunkIndex")
            outputStream = FileOutputStream(tempFile)
        }
        inputStream.close()
        outputStream.close()
        return chunks
    }

    private fun calcFileCrcValue(file: File): String {
        val inputStream = FileInputStream(file)
        val checksum = CRC32()
        val buffer = ByteArray(1024)
        var bytesRead: Int
        while (true) {
            bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break
            checksum.update(buffer, 0, bytesRead)
        }
        inputStream.close()

        val crcValue = checksum.value
        return crcValue.toHexString()
    }
}