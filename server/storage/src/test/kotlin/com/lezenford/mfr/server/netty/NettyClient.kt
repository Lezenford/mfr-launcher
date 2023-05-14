package com.lezenford.mfr.server.netty

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel

class NettyClient(
    port: Int,
    workGroup: NioEventLoopGroup,
    private val factory: HandlerFactory,
    private val handler: ChannelHandler
) {
    val channel: Channel = Bootstrap()
        .group(workGroup)
        .channel(NioSocketChannel::class.java)
        .handler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                ch.pipeline().addLast(
                    factory.lengthFieldBasedFrameDecoder(),
                    factory.lengthFieldPrepender(),
                    factory.requestDecoder(),
                    factory.responseEncoder(),
                    handler
                )
            }
        })
        .connect("localhost", port).sync().channel()
}