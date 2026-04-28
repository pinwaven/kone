package poct.device.app.serial.ctl

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.channel.ChannelProgressivePromise
import io.netty.channel.ChannelPromise
import io.netty.channel.DefaultChannelProgressivePromise
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import io.netty.util.concurrent.EventExecutor
import poct.device.app.serial.SerialPort
import java.net.SocketAddress

class CtlChannelContextProxy(private val serialPort: SerialPort<CtlSerialMessage>) :
    ChannelHandlerContext {
    @Deprecated("Use {@link Channel#attr(AttributeKey)}")
    override fun <T : Any?> attr(key: AttributeKey<T>?): Attribute<T> {
        throw RuntimeException("unsupported")
    }

    @Deprecated("Use Channel.hasAttr(AttributeKey)")
    override fun <T : Any?> hasAttr(key: AttributeKey<T>?): Boolean {
        throw RuntimeException("unsupported")
    }

    override fun fireChannelRegistered(): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun fireChannelUnregistered(): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun fireChannelActive(): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun fireChannelInactive(): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun fireExceptionCaught(cause: Throwable?): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun fireUserEventTriggered(evt: Any?): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun fireChannelRead(msg: Any?): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun fireChannelReadComplete(): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun fireChannelWritabilityChanged(): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun bind(localAddress: SocketAddress?): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun bind(localAddress: SocketAddress?, promise: ChannelPromise?): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun connect(remoteAddress: SocketAddress?): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun connect(
        remoteAddress: SocketAddress?,
        localAddress: SocketAddress?,
    ): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun connect(remoteAddress: SocketAddress?, promise: ChannelPromise?): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun connect(
        remoteAddress: SocketAddress?,
        localAddress: SocketAddress?,
        promise: ChannelPromise?,
    ): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun disconnect(): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun disconnect(promise: ChannelPromise?): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun close(): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun close(promise: ChannelPromise?): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun deregister(): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun deregister(promise: ChannelPromise?): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun read(): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun write(msg: Any?): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun write(msg: Any?, promise: ChannelPromise?): ChannelFuture {
        throw RuntimeException("unsupported")
    }

    override fun flush(): ChannelHandlerContext {
        throw RuntimeException("unsupported")
    }

    override fun writeAndFlush(msg: Any?, promise: ChannelPromise?): ChannelFuture {
        throw java.lang.RuntimeException("unsupported")
    }

    override fun writeAndFlush(msg: Any?): ChannelFuture {
        if (msg is CtlSerialMessage) {
            serialPort.send(msg)
        }
        return DefaultChannelProgressivePromise(null)
    }

    override fun newPromise(): ChannelPromise {
        throw java.lang.RuntimeException("unsupported")
    }

    override fun newProgressivePromise(): ChannelProgressivePromise {
        throw java.lang.RuntimeException("unsupported")
    }

    override fun newSucceededFuture(): ChannelFuture {
        throw java.lang.RuntimeException("unsupported")
    }

    override fun newFailedFuture(cause: Throwable?): ChannelFuture {
        throw java.lang.RuntimeException("unsupported")
    }

    override fun voidPromise(): ChannelPromise {
        throw java.lang.RuntimeException("unsupported")
    }

    override fun channel(): Channel {
        throw RuntimeException("unsupported")
    }

    override fun executor(): EventExecutor {
        throw RuntimeException("unsupported")
    }

    override fun name(): String {
        throw RuntimeException("unsupported")
    }

    override fun handler(): ChannelHandler {
        throw RuntimeException("unsupported")
    }

    override fun isRemoved(): Boolean {
        throw RuntimeException("unsupported")
    }

    override fun pipeline(): ChannelPipeline {
        throw RuntimeException("unsupported")
    }

    override fun alloc(): ByteBufAllocator {
        throw RuntimeException("unsupported")
    }
}