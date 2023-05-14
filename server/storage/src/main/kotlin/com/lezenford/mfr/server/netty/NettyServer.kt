package com.lezenford.mfr.server.netty

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.netty.component.ChannelExceptionHandler
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.WriteBufferWaterMark
import io.netty.channel.group.ChannelGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.util.AttributeKey
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.*
import javax.annotation.PreDestroy
import kotlin.system.exitProcess

val CLIENT_ID: AttributeKey<UUID> = AttributeKey.valueOf(UUID::class.java, "CLIENT_ID")

@Component
class NettyServer(
    private val clients: ChannelGroup,
    private val factory: HandlerFactory,
    private val properties: ServerSettingProperties
) {
    private val bossGroup = NioEventLoopGroup(1)
    private val workerGroup = NioEventLoopGroup()
    private val serverChannel: Channel by lazy {
        ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    properties.netty.security?.also {
                        val sslContext = SslContextBuilder.forServer(it.cert.toFile(), it.key.toFile()).build()
                        ch.pipeline().addLast(sslContext.newHandler(ch.alloc()))
                    }
                    ch.pipeline().addLast(
                        factory.timeoutStateHandler(),
                        factory.trafficShapingHandler(),
                        factory.lengthFieldBasedFrameDecoder(),
                        factory.lengthFieldPrepender(),
                        factory.requestDecoder(),
                        factory.responseEncoder(),
                        factory.serverInboundHandler()
                    ).addLast(ChannelExceptionHandler.NAME, factory.channelExceptionHandler())
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark(1024 * 1024, 1024 * 1024 * 3))
            .bind(properties.netty.port).sync().channel().apply {
                properties.netty.security?.also {
                    log.info("Netty ssl settings successfully installed")
                } ?: log.info("Netty ssl settings not found")
                log.info("Netty started on port(s): ${this.localAddress().toString().substringAfterLast(":")} (tcp)")
            }
    }


    @EventListener(ApplicationStartedEvent::class)
    fun start() {
        try {
            serverChannel
        } catch (e: Exception) {
            log.error("Error while start Netty server", e)
            exitProcess(0)
        }
    }

    @PreDestroy
    fun stop() {
        log.info("Stopping Netty server")
        clients.close().sync()
        serverChannel.close().sync()
        bossGroup.shutdownGracefully().sync()
        workerGroup.shutdownGracefully().sync()
        log.info("Netty server is finished")
    }

    companion object {
        private val log by Logger()
    }
}