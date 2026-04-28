package poct.device.app.serial

import info.szyh.common4.lang.ByteUtils
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import timber.log.Timber
import tp.xmaihh.serialport.SerialHelper
import tp.xmaihh.serialport.bean.ComBean
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class SerialPort<T : SerialMessage>(
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
    private val receiverBuf: ByteBuf = Unpooled.buffer(recBufSize)

    // 循环标记
    private var flag: Boolean = false

    init {
        Timber.d("init: $port $baudRate")
        serialHelper = object : SerialHelper(port, baudRate) {
            override fun onDataReceived(p0: ComBean?) {
                if (p0 != null && p0.bRec.isNotEmpty()) {
                    receiverBuf.discardReadBytes()
                    // 容量超过缓存，则清除
                    if (receiverBuf.readableBytes() > recMaxSize) {
                        receiverBuf.clear()
                    }
                    Timber.d("received: ${ByteUtils.toPrettyHex(p0.bRec)}")
                    receiverBuf.writeBytes(p0.bRec, 0, p0.bRec.size)
                    onDataReceived(receiverBuf)
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
                    Timber.d("send    : ${ByteUtils.toPrettyHex(byteBuf)}")
                    serialHelper.send(byteBuf.array())
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