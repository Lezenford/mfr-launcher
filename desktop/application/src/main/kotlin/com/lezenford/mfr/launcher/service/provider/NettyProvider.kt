package com.lezenford.mfr.launcher.service.provider

import com.lezenford.mfr.common.protocol.http.dto.Client
import com.lezenford.mfr.common.protocol.netty.RequestChangeState
import com.lezenford.mfr.common.protocol.netty.RequestGameFilesMessage
import com.lezenford.mfr.common.protocol.netty.RequestLauncherFilesMessage
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.netty.FileData
import com.lezenford.mfr.launcher.netty.REQUESTED_FILES
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.factory.HandlerFactory
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContextBuilder
import io.netty.util.concurrent.GlobalEventExecutor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.springframework.stereotype.Component
import java.nio.file.Path
import javax.annotation.PreDestroy

@Component
class NettyProvider(
    private val nettyClientWorkGroup: NioEventLoopGroup,
    private val properties: ApplicationProperties,
    private val factory: HandlerFactory,
) {
    private val channelsPool: ChannelGroup = DefaultChannelGroup(GlobalEventExecutor.INSTANCE)

    suspend fun downloadLauncher(target: Path, launcher: Client) {
        MUTEX.withLock {
            State.nettyDownloadActive.emit(true)

            val channel = prepareConnection()
            val data = FileData(id = 0, path = target, size = launcher.size, md5 = launcher.md5)
            channel.attr(REQUESTED_FILES).set(listOf(data))
            channel.writeAndFlush(RequestLauncherFilesMessage(systemType = properties.platform))

            channel.closeFuture().sync()

            State.nettyDownloadActive.emit(false)
        }
    }

    suspend fun downloadGameFiles(files: List<FileData>) {
        MUTEX.withLock {
            State.nettyDownloadActive.emit(true)

            val connectionsCount = connectionsCount(files)
            val connections = Array(connectionsCount) { prepareConnection() }
            val split = splitFiles(files, connectionsCount)
            connections.onEachIndexed { index, channel ->
                val list = split[index]
                channel.attr(REQUESTED_FILES).set(list)
                channel.writeAndFlush(RequestGameFilesMessage(properties.clientId, list.map { it.id }))
            }

            connections.forEach { it.closeFuture().sync() }

            State.nettyDownloadActive.emit(false)
        }
    }

    fun pause() {
        channelsPool.filter { it.isActive }.forEach { it.writeAndFlush(RequestChangeState(false)) }
    }

    fun resume() {
        channelsPool.filter { it.isActive }.forEach { it.writeAndFlush(RequestChangeState(true)) }
    }

    fun limit(maxReadBandwidth: Long) {
        factory.channelTrafficShapingHandler().readLimit = maxReadBandwidth
    }

    private fun connectionsCount(files: List<FileData>): Int =
        maxOf(1, minOf(files.size, properties.server.connectionCount))

    private fun prepareConnection(): Channel = Bootstrap()
        .group(nettyClientWorkGroup)
        .channel(NioSocketChannel::class.java)
        .handler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                ch.pipeline().addLast(
                    SslContextBuilder.forClient().build().newHandler(ch.alloc()),
                    factory.channelTrafficShapingHandler(),
                    factory.lengthFieldBasedFrameDecoder(),
                    factory.lengthFieldPrepender(),
                    factory.requestDecoder(),
                    factory.responseEncoder(),
                    factory.inboundHandler(),
                    factory.exceptionHandler()
                )
            }
        })
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
        .option(ChannelOption.TCP_NODELAY, true).connect(properties.server.address, properties.server.tcpPort).sync()
        .channel().also { channelsPool.add(it) }

    private fun splitFiles(files: List<FileData>, connections: Int): List<List<FileData>> {
        class ChannelContent {
            val list = mutableListOf<FileData>()
            var totalSize = 0L
        }

        val contents = Array(connections) { ChannelContent() }
        val totalBytes = files.sumOf { it.size }

        files.sortedByDescending { it.size }.forEachIndexed { index, file ->
            val content = contents[index % contents.size].takeIf { totalBytes - it.totalSize > file.size }
                ?: contents.find { totalBytes - it.totalSize > file.size } ?: contents.last()
            content.list.add(file)
            content.totalSize += file.size
        }

        return contents.map { it.list }
    }

    @PreDestroy
    fun closeConnections() {
        channelsPool.filter { it.isActive }.forEach { it.close() }
    }

    companion object {
        private val MUTEX = Mutex()
    }
}