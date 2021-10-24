package com.lezenford.mfr.server.netty

import com.fasterxml.jackson.databind.ObjectMapper
import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.netty.component.RequestInboundHandler
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.WriteBufferWaterMark
import io.netty.channel.group.ChannelGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.bytes.ByteArrayDecoder
import io.netty.handler.codec.bytes.ByteArrayEncoder
import io.netty.handler.ssl.SslContextBuilder
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.ContextStoppedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.netty.RequestDecoder
import ru.fullrest.mfr.common.netty.ResponseEncoder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Provider

@Component
class NettyServer(
    private val properties: ServerSettingProperties,
    private val objectMapper: ObjectMapper,
    private val requestInboundHandlerProvider: Provider<RequestInboundHandler>,
    private val channels: ChannelGroup
) {
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private var serverChannel: Channel? = null

    @EventListener(ApplicationStartedEvent::class)
    fun start() = executorService.execute {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val serverChannel = ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(ch: SocketChannel) {
                        properties.netty.security?.also {
                            val sslContext = SslContextBuilder.forServer(it.cert.toFile(), it.key.toFile()).build()
                            ch.pipeline().addLast(sslContext.newHandler(ch.alloc()))
                        }
                        ch.pipeline().addLast(
                            LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                            LengthFieldPrepender(3),
                            ByteArrayDecoder(),
                            ByteArrayEncoder(),
                            RequestDecoder(objectMapper),
                            ResponseEncoder(objectMapper),
                            requestInboundHandlerProvider.get()
                        )
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 120000)
                .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark(1024 * 1024, 1024 * 1024 * 3))
                .bind(properties.netty.port).sync().channel()
            properties.netty.security?.also {
                log.info("Netty ssl settings successfully installed")
            } ?: log.info("Netty ssl settings not found")
            log.info("Netty started on port(s): ${properties.netty.port} (tcp)")
            serverChannel.closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
            log.info("Netty server is finished")
            executorService.shutdown()
        }
    }

    @EventListener(ContextStoppedEvent::class)
    fun stop() {
        log.info("Stopping Netty server")
        channels.forEach { it.close() }
        serverChannel?.close()
        executorService.shutdownNow()
        log.info("Netty server successfully stopped")
    }

    companion object {
        private val log by Logger()
    }
}