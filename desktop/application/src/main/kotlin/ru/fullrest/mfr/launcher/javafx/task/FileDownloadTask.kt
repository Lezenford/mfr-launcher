package ru.fullrest.mfr.launcher.javafx.task

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.codec.bytes.ByteArrayDecoder
import io.netty.handler.codec.bytes.ByteArrayEncoder
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.traffic.ChannelTrafficShapingHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.api.rest.Content
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.format
import ru.fullrest.mfr.common.netty.RequestDecoder
import ru.fullrest.mfr.common.netty.ResponseEncoder
import ru.fullrest.mfr.javafx.component.ProgressBar
import ru.fullrest.mfr.launcher.component.ApplicationStatus
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.netty.InboundHandler
import java.nio.file.Path

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class FileDownloadTask(
    private val objectMapper: ObjectMapper,
    private val nettyClientWorkGroup: NioEventLoopGroup,
    private val applicationProperties: ApplicationProperties,
    private val applicationStatus: ApplicationStatus
) {

    suspend fun execute(
        requestFiles: MutableMap<Int, Content.Category.Item.File>,
        progressBar: ProgressBar,
        targetFolder: Path = applicationProperties.gameFolder
    ) {
        if (applicationStatus.onlineMode.value) {
            var channel: Channel? = null
            try {
                channel = createNettyClient(
                    requestFiles,
                    targetFolder,
                    { progressBar.updateDescription("Скорость скачивания: ${convertTrafficValue(it)}") },
                    { progressBar.updateProgress(it.first, it.second) })
                while (channel.isActive) {
                    delay(100)
                }
            } finally {
                channel?.also { it.close() }
            }
        }
    }

    private fun convertTrafficValue(bytes: Long): String {
        return if (bytes > 1024) {
            val kilobytes = bytes / 1024.0
            if (kilobytes > 1024.0) {
                val megabytes = kilobytes / 1024.0
                "${megabytes.format(2)} МБ/с"
            } else {
                "${kilobytes.format(2)} КБ/с"
            }
        } else {
            "$bytes Б/с"
        }
    }

    private fun createNettyClient(
        requestFiles: MutableMap<Int, Content.Category.Item.File>,
        targetPath: Path,
        trafficObserver: (Long) -> Unit,
        progressObserver: (Pair<Long, Long>) -> Unit
    ): Channel {
        val channelTrafficShapingHandler = ChannelTrafficShapingHandler(1000)
        return Bootstrap()
            .group(nettyClientWorkGroup)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        SslContextBuilder.forClient().build().newHandler(ch.alloc()),
                        channelTrafficShapingHandler,
                        LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3),
                        LengthFieldPrepender(3),
                        ByteArrayDecoder(),
                        ByteArrayEncoder(),
                        RequestDecoder(objectMapper),
                        ResponseEncoder(objectMapper),
                        InboundHandler(
                            requestFiles,
                            applicationProperties.clientId,
                            targetPath,
                            progressObserver
                        )
                    )
                }
            })
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
            .connect(applicationProperties.server.address, applicationProperties.server.tcpPort).sync().channel()
            .also {
                CoroutineScope(Dispatchers.Default).launch {
                    while (it.isActive) {
                        delay(1000)
                        trafficObserver(channelTrafficShapingHandler.trafficCounter().lastReadBytes())
                    }
                }
            }
    }

    companion object {
        private val log by Logger()
    }
}