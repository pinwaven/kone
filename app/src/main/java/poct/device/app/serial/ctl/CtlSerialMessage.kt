package poct.device.app.serial.ctl

import info.szyh.common4.codec.CrcUtils
import info.szyh.common4.lang.ByteUtils
import info.szyh.common4.reflect.ReflectUtils
import info.szyh.socket.SocketConstants
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import poct.device.app.serial.SerialMessage
import poct.device.app.serial.SerialQueryParams
import timber.log.Timber


class CtlSerialMessage : SerialMessage {

    /**
     * 类型，响应/控制/反馈
     */
    private var _type: Byte = 0

    /**
     * 指令
     */
    var cmd: Byte = 0

    private var _byteData: ByteArray? = null
    private var _paramData: SerialQueryParams? = null

    /**
     * 二进制数据
     */
    var byteData: ByteArray?
        get() {
            return _byteData
        }
        set(value) {
            _byteData = value
            _paramData = null
        }

    var paramData: SerialQueryParams
        get() {
            if (_paramData == null) {
                _paramData = if (_byteData == null) EMPTY_PARAM else SerialQueryParams(
                    String(_byteData!!),
                    false,
                    ","
                )
            }
            return _paramData!!
        }
        set(value) {
            _paramData = value
            if (_paramData == null) {
                _byteData = null
            } else {
                _byteData = _paramData!!.toQueryString().toByteArray()
            }
        }

    /**
     * 地址
     */
    var address: Short = 0
    override fun getUid(): String {
        return "$cmd@$sid"
    }

    override fun getDelay(): Int {
        return paramData.getParameter(CtlConstants.PARAM_DELAY)?.toInt() ?: 0
    }

    override fun getSid(): Int {
        return paramData.getParameter(CtlConstants.PARAM_SID)?.toInt() ?: 0
    }

    override fun getType(): Byte {
        return _type
    }

    fun setType(type: Byte) {
        _type = type
    }

    /**
     * 返回 [ByteBuf]，通过 [Unpooled.buffer] 创建，然后写入数据
     *
     *
     * 如果已经有byte[]，可以使用 [Unpooled.wrappedBuffer]
     *
     * @return
     */
    override fun toByteBuf(): ByteBuf {
        val buffer = Unpooled.buffer()
        buffer.writeBytes(HEADER)
        // A
        buffer.writeShort(address.toInt())
        // B
        val lengthWriterIndex = buffer.writerIndex()
        buffer.writeShort(0)
        // C
        buffer.writeByte(_type.toInt())
        // D
        buffer.writeByte(cmd.toInt())
        // E
        if (byteData != null) {
            buffer.writeBytes(byteData)
        }

        // 设置真实长度
        val tmpWriterIndex = buffer.writerIndex()
        buffer.writerIndex(lengthWriterIndex)
        // 当前长度  - 2header  - 2地址 - 4长度 + 2CRC
        buffer.writeShort(tmpWriterIndex - 4 - LENGTH_BYTES_CT + CRC_BYTES_CT)
        buffer.writerIndex(tmpWriterIndex)
        // F 校验
        val bytes = ByteArray(buffer.readableBytes() - CRC_BYTES_CT)
        // 去除header
        buffer.readShort()
        buffer.readBytes(bytes)
        buffer.writeBytes(CrcUtils.crc16(bytes))
        buffer.writeBytes(FOOTER)
        buffer.readerIndex(0)
        return buffer
    }

    private fun clone(): CtlSerialMessage {
        val clone: CtlSerialMessage = ReflectUtils.newObject(javaClass)
        clone.address = address
        clone._type = _type
        clone.cmd = cmd
        if (byteData != null) {
            clone.byteData = byteData
        }
        return clone
    }

    override fun toFeedback(): CtlSerialMessage {
        val clone: CtlSerialMessage = ReflectUtils.newObject(javaClass)
        clone.address = address
        clone._type = SocketConstants.TYPE_FEEDBACK
        clone.cmd = cmd
        return clone
    }

    override fun toResponse(): CtlSerialMessage {
        val clone: CtlSerialMessage = clone()
        clone._type = SocketConstants.TYPE_RESPONSE
        return clone
    }

    override fun toResponseWithoutData(): CtlSerialMessage {
        val clone: CtlSerialMessage = clone()
        clone._type = SocketConstants.TYPE_RESPONSE
        clone.byteData = null
        return clone
    }

    companion object {
        private val HEADER = byteArrayOf(0xFC.toByte(), 0x03)
        private val FOOTER = byteArrayOf(0xEB.toByte(), 0x90.toByte())
        private val MAX_BYTES_CT = 256
        private val MIN_BYTES_CT = 12

        /**
         * 长度字段所占的字节数
         */
        private val LENGTH_BYTES_CT = 2

        /**
         * CRC所占的字节数
         */
        private val CRC_BYTES_CT = 2
        private val EMPTY_PARAM = SerialQueryParams("", false, ",")
        fun fromByteBuf(byteBuf: ByteBuf): CtlSerialMessage? {
            val overLeed = byteBuf.readableBytes() > MAX_BYTES_CT
            if (overLeed) {
                byteBuf.clear()
                return null
            }
            return find(byteBuf)
        }

        private fun find(msg: ByteBuf): CtlSerialMessage? {
            // 00 01 00 00 00 07 FF 04 04 00 00 2C 03
            // 获取前9个字节以外的所有字节， 00 01 00 00 为包头， 5-6位为数据长度
            // 数据包最小长度：12
            if (msg.readableBytes() < MIN_BYTES_CT) {
                return null
            }
            // 查找包头
            val headerIndex = ByteUtils.indexOfBytes(msg, HEADER)
            if (headerIndex < 0) {
                // 没找到包头，舍弃所有字节
                msg.clear()
                return null
            }
            msg.readerIndex(headerIndex)
            // 包头
            msg.readerIndex(headerIndex + HEADER.size)
            val bytes2 = ByteArray(2)
            // A 地址，忽略
            val address = msg.readShort()
            // B 长度
            val dataLength = msg.readShort()
            // 数据长度需要超过内容长度+报文尾长度
            if (msg.readableBytes() < dataLength + FOOTER.size) {
                Timber.w("数据不全，等下一轮解析")
                // 数据不全，恢复数据
                msg.readerIndex(headerIndex)
                return null
            }
            // C 类型
            val type = msg.readByte()
            // D 指令
            val cmd = msg.readByte()
            // E 数据
            var data: ByteArray? = null
            // 内部数据长度 - 1类型 - 1指令 - 2CRC
            val innerDataLength = dataLength - 2 - CRC_BYTES_CT
            if (innerDataLength > 0) {
                data = ByteArray(innerDataLength)
                msg.readBytes(data)
            }
            // F 校验
            val crc = ByteArray(2)
            msg.readBytes(crc)
            // 包尾
            msg.readBytes(bytes2)
            if (!ByteUtils.equals(bytes2, FOOTER)) {
                // 如果包尾不符合，直接抛弃
                return null
            }
            val beforeCheckCrc = msg.readerIndex()
            // 恢复到去除包头部分，读取A-E
            msg.readerIndex(headerIndex + HEADER.size)
            // 增加地址与长度，去除CRC
            val checkBytes = ByteArray(dataLength + 2 + LENGTH_BYTES_CT - CRC_BYTES_CT)
            msg.readBytes(checkBytes)
            val bytes = CrcUtils.crc16(checkBytes)
            if (!ByteUtils.equals(bytes, crc)) {
                Timber.w("数据校验失败")
                // CRC校验不通过
                return null
            }
            // 恢复检查前
            msg.readerIndex(beforeCheckCrc)
            val socketMessage = CtlSerialMessage()
            socketMessage.cmd = cmd
            socketMessage._type = type
            socketMessage.byteData = data
            socketMessage.address = address
            return socketMessage
        }
    }
}