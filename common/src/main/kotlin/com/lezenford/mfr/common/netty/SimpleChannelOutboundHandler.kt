package com.lezenford.mfr.common.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandler
import io.netty.channel.ChannelPromise
import java.net.SocketAddress

interface SimpleChannelOutboundHandler : ChannelOutboundHandler {
    override fun bind(ctx: ChannelHandlerContext, localAddress: SocketAddress, promise: ChannelPromise) {
        ctx.bind(localAddress, promise)
    }

    override fun connect(
        ctx: ChannelHandlerContext,
        remoteAddress: SocketAddress,
        localAddress: SocketAddress,
        promise: ChannelPromise
    ) {
        ctx.connect(remoteAddress, localAddress, promise)
    }

    override fun disconnect(ctx: ChannelHandlerContext, promise: ChannelPromise) {
        ctx.disconnect(promise)
    }

    override fun close(ctx: ChannelHandlerContext, promise: ChannelPromise) {
        ctx.close(promise)
    }

    override fun deregister(ctx: ChannelHandlerContext, promise: ChannelPromise) {
        ctx.deregister(promise)
    }

    override fun read(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        ctx.write(msg, promise)
    }

    override fun flush(ctx: ChannelHandlerContext) {
        ctx.flush()
    }
}