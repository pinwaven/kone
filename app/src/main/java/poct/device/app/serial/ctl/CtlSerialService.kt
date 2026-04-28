package poct.device.app.serial.ctl

import info.szyh.socket.comm.SocketMessageCallbackAdapter
import info.szyh.socket.comm.SocketMessageSender
import info.szyh.socket.comm.SocketMessageSenderHolder
import info.szyh.socket.handler.SocketMessage
import info.szyh.socket.utils.SocketCommUtils
import io.netty.buffer.ByteBuf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import poct.device.app.AppParams
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.serial.SerialConnectEvent
import poct.device.app.serial.SerialMessageCallbackAdapter
import poct.device.app.serial.SerialPort
import poct.device.app.utils.app.AppEventUtils
import timber.log.Timber

class CtlSerialService {
    private val senderConfig = SocketMessageSender.SenderConfig(1, 0, 1)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var latest: Long? = null
    private var count: Int = 0

    // 串口
    private val serialPort = SerialPort<CtlSerialMessage>(
        port = "/dev/ttyS1",
        baudRate = 115200
    ) { byteBuf: ByteBuf ->
        var message: CtlSerialMessage? = null
        while ((CtlSerialMessage.fromByteBuf(byteBuf).also { message = it }) != null) {
            handleMessage(message!!)
        }
    }

    private fun handleMessage(message: CtlSerialMessage) {
        latest = System.currentTimeMillis()
        reply(message)
        SocketCommUtils.resolve(message, context, CtlConstants.UID, senderHolder)
    }

    // 定时检查标记
    private var flag = false

    // 发送指令收集
    private val senderHolder = SocketMessageSenderHolder<SocketMessage>()

    // 模拟socket通道
    private val context = CtlChannelContextProxy(serialPort)

    fun start() {
        SocketCommUtils.registerReceivedEventClass(
            CtlSerialMessage::class.java,
            CtlSerialMessageEvent::class.java
        )
        AppEventUtils.register(this)
        flag = true
        startRetryThread()
        serialPort.open()
        send(CtlCommands.handleShake())
        startReconnectThread()
    }

    private fun startReconnectThread() {
        // TODO 串口失联时，补救逻辑
//        if (AppParams.devMock) {
//            return
//        }

        latest = System.currentTimeMillis()
        Thread {
            while (flag) {
//                每5秒检查一次
                Thread.sleep(5000)
                // 超过15秒，则需要连接，并发布事件-串口断开
                val timeNoRes = System.currentTimeMillis() - latest!!
                if (timeNoRes > 15000) {
                    AppEventUtils.publishEvent(SerialConnectEvent(if (timeNoRes > 30000) -1 else 0))
                    reconnect()
                    send(CtlCommands.handleShake())
                }

            }
        }.start()
    }

    fun reconnect() {
        serialPort.reconnect()
    }

    fun send(
        message: CtlSerialMessage,
        callback: SerialMessageCallbackAdapter<CtlSerialMessage>? = null,
    ) {
        // TODO 串口失联时，补救逻辑
//        if (AppParams.devMock) {
//            Thread.sleep(200)
//            coroutineScope.launch {
//                delay(2000)
//                callback?.success(
//                    message.toFeedback(),
//                    SocketMessageSender(CtlConstants.UID, message),
//                    this
//                )
//            }
//            return
//        }

        SocketMessageSender(
            CtlConstants.UID,
            message,
            object : SocketMessageCallbackAdapter<CtlSerialMessage>() {
                override fun beforeTry(sender: SocketMessageSender<CtlSerialMessage>?) {
                    coroutineScope.launch {
                        callback?.beforeTry(sender, this)
                    }
                }

                override fun afterTry(sender: SocketMessageSender<CtlSerialMessage>?) {
                    coroutineScope.launch {
                        callback?.afterTry(sender, this)
                    }
                }

                override fun delay(
                    feedback: CtlSerialMessage?,
                    sender: SocketMessageSender<CtlSerialMessage>?,
                ) {
                    coroutineScope.launch {
                        Timber.d("DELAY triggered")
                        callback?.delay(feedback!!, sender, this)
                    }
                }

                override fun error(
                    sender: SocketMessageSender<CtlSerialMessage>?,
                    e: java.lang.Exception?,
                ) {
                    coroutineScope.launch {
                        Timber.d("ERROR triggered")
                        callback?.error(sender, e, this)
                    }
                }

                override fun complete(sender: SocketMessageSender<CtlSerialMessage>?) {
                    coroutineScope.launch {
                        callback?.complete(sender, this)
                    }
                }

                override fun success(
                    feedback: CtlSerialMessage?,
                    sender: SocketMessageSender<CtlSerialMessage>?,
                ) {
                    coroutineScope.launch {
                        Timber.d("SUCCESS triggered")
                        callback?.success(feedback!!, sender, this)
                    }
                }
            },
            senderConfig
        ).trySend(context, senderHolder)
    }

    private fun reply(message: CtlSerialMessage) {
        SocketCommUtils.reply(message, context, senderHolder)
    }

    fun stop() {
        flag = false
        serialPort.close()
        AppEventUtils.unregister(this)
    }

    private fun startRetryThread() {
        Thread {
            while (flag) {
                Thread.sleep(500)
                SocketCommUtils.checkResent(context, senderHolder, 3)
            }
        }.start()
    }

    @Subscribe
    fun handleSerialEvent(event: CtlSerialMessageEvent) {
//        根据反馈更新数据
        val message = event.message
        if (message.cmd == CtlConstants.CMD_HANDSHAKE || message.cmd == CtlConstants.CMD_READ_SYS) {
            AppEventUtils.publishEvent(SerialConnectEvent(1))
            // 大板
            val version0 = message.paramData.getParameter(CtlConstants.PARAM_VERSION0) ?: "V0.0.0"
            // 上报版本
            coroutineScope.launch {
                val configInfo =
                    SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoBean::class)
                configInfo.hardware = version0
                SysConfigService.saveBean(ConfigInfoBean.PREFIX, configInfo)
//                SysConfigService.reportVersion()
            }
            Timber.d("save version and report: $version0")
        }
        if (message.cmd == CtlConstants.CMD_TEST_RESPONSE) {
            val testSid = message.paramData.getParameter(CtlConstants.PARAM_TEST_SID)
            AppParams.testCount = AppParams.testCount + 1
            Timber.w("=====testSid=====%s", testSid)
            Timber.w("=====testCount=====%s", AppParams.testCount)
        }
    }
}