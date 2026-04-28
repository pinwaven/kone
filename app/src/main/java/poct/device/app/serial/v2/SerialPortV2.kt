package poct.device.app.serial.v2

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import poct.device.app.utils.common.ByteUtils
import timber.log.Timber
import tp.xmaihh.serialport.SerialHelper
import tp.xmaihh.serialport.bean.ComBean
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class SerialPortV2<T : SerialMessageV2>(
    // 端口
    var port: String,
    // 波特率
    var baudRate: Int = 9600,
    // 停止位
    var stopBits: Int = 1,
    // 数据位
    var dataBits: Int = 8,
    // 校验位
    var parity: Int = 0,
    // 流控
    var flowCon: Int = 0,
    var recBufSize: Int = 3 * 1024 * 1024,
    // 消息处理
    var onDataReceived: (ByteBuf) -> Unit,
) {
    // 串口帮助类
    private val serialHelper: SerialHelper
    private val recMaxSize: Int = (recBufSize * 0.75).toInt()

    // 消息接收缓存队列
    private var sendDataBlock: BlockingQueue<T> = LinkedBlockingQueue()

    // 循环标记
    private var flag: Boolean = false

    init {
        Timber.d("init: $port $baudRate")
        serialHelper = object : SerialHelper(port, baudRate) {
            override fun onDataReceived(p0: ComBean?) {
                if (p0 != null && p0.bRec.isNotEmpty()) {
                    val endIndex = p0.bRec.indexOfLast { it != 0x00.toByte() }
                    val data = p0.bRec.copyOfRange(0, endIndex + 1)
                    Timber.d("received: ${ByteUtils.toPrettyHex(data)}")
                    Timber.d("received str: ${data.toString(Charsets.UTF_8)}")

                    val receiverBuf: ByteBuf = Unpooled.buffer(data.size)
                    receiverBuf.writeBytes(data)
                    onDataReceived(receiverBuf)
                    close()
                }
            }
        }
        serialHelper.stopBits = stopBits
        serialHelper.dataBits = dataBits
        serialHelper.parity = parity
        serialHelper.flowCon = flowCon
    }

    fun open() {
        Timber.d("try to open serial[$port] ")
        try {
            serialHelper.open()
            flag = true
            startSendThread()
            Timber.d("serial[$port] opened")
        } catch (e: Exception) {
            Timber.e("serial[$port] failed: ${e.message}")
        }
    }

    private fun startSendThread() {
        Thread {
            while (flag) {
                try {
                    val msg = sendDataBlock.poll()!!
                    val byteBuf = msg.toByteBuf()
                    val hexMsg = ByteUtils.toPrettyHexWithoutSep(byteBuf)
                    Timber.d("send    : $hexMsg")
                    serialHelper.sendHex(hexMsg)
                    Thread.sleep(10)
                } catch (_: Exception) {
                }
            }
        }.start()
    }

    fun close() {
        try {
            serialHelper.close()
        } catch (_: Exception) {
        }
        flag = false
        Timber.d("serial[$port] closed")
    }

    fun reconnect() {
        serialHelper.close()
        serialHelper.open()
    }

    fun send(data: T) {
        Timber.d("ser send: $data")
        sendDataBlock.put(data)
    }
}