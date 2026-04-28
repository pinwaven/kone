package poct.device.app.serial.v2

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import poct.device.app.serial.SerialQueryParams
import poct.device.app.serial.v2.ctl.CtlConstantsV2
import poct.device.app.utils.common.ByteUtils
import poct.device.app.utils.common.CrcUtils
import poct.device.app.utils.common.ReflectUtils
import timber.log.Timber

class CtlSerialMessageV2 : SerialMessageV2 {
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
        }

    fun toHexString(): String {
        val byteBuf = this.toByteBuf()
        return ByteUtils.toPrettyHexWithoutSep(byteBuf)
    }

    override fun getUid(): String {
        return "$cmd@$sid"
    }

    override fun getSid(): Int {
        return paramData.getParameter(CtlConstantsV2.PARAM_SID)?.toInt() ?: 0
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
        // 预计算总长度避免扩容
        val estimatedLength = HEADER.size + 1 + (byteData?.size ?: 0) + CRC_BYTES_CT
        val buffer = Unpooled.buffer(estimatedLength)
        buffer.writeBytes(HEADER)

        buffer.writeByte(cmd.toInt())
        if (byteData != null) {
            buffer.writeBytes(byteData)
        }

        // CRC校验
        // 1. 复制需要计算CRC的数据（排除Header）
        val crcData = ByteArray(buffer.readableBytes())  // 减去Header长度
        buffer.getBytes(0, crcData)  // 从Header后开始复制

        // 2. 计算CRC并写入
        val crc = CrcUtils.crc8(crcData)
        buffer.writeBytes(crc)

        // 3. 重置读指针（可选，取决于后续使用场景）
        buffer.readerIndex(0)
        return buffer
    }

    override fun toString(): String {
        return "cmd：" + this.cmd.toString() + " byteData：" + (this.byteData?.let {
            this.byteData!!.joinToString(" ") {
                "%02X".format(
                    it
                )
            }
        } ?: "null")
    }

    private fun clone(): CtlSerialMessageV2 {
        val clone: CtlSerialMessageV2 = ReflectUtils.newObject(javaClass)
        clone.cmd = cmd
        if (byteData != null) {
            clone.byteData = byteData
        }
        return clone
    }

    companion object {
        const val MAX_BYTES_CT = 3 * 1024 * 1024 * 0.75
        private val HEADER = byteArrayOf(0x21.toByte())

        /**
         * CRC所占的字节数
         */
        private const val CRC_BYTES_CT = 1

        private val EMPTY_PARAM = SerialQueryParams("", false, ",")

        fun fromByteBuf(byteBuf: ByteBuf): CtlSerialMessageV2? {
            val overLeed = byteBuf.readableBytes() > MAX_BYTES_CT
            if (overLeed) {
                byteBuf.clear()
                return null
            }
            return this.check(byteBuf)
        }

        private fun check(msg: ByteBuf): CtlSerialMessageV2? {
            // 查找包头
            val headerIndex = ByteUtils.indexOfBytes(msg, HEADER)
            if (headerIndex < 0) {
                // 没找到包头，舍弃所有字节
                msg.clear()
                return null
            }

            // 数据
            var data: ByteArray? = null

            // 内部数据长度 - 1CRC
            val innerDataLength = msg.readableBytes() - CRC_BYTES_CT
            if (innerDataLength > 0) {
                data = ByteArray(innerDataLength)
                msg.readBytes(data)
            }

            // 校验
            val crc = ByteArray(1)
            msg.readBytes(crc)

            // 去除CRC
            val bytes = CrcUtils.crc8(data)
            if (!ByteUtils.equals(bytes, crc)) {
                Timber.w("数据校验失败。${ByteUtils.toPrettyHexWithoutSep(msg)}")
                // CRC校验不通过
                return null
            }

            val socketMessage = CtlSerialMessageV2()
            socketMessage.byteData = data
            return socketMessage
        }
    }
}