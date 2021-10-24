package com.lezenford.mfr.server.netty.component

import com.lezenford.mfr.server.component.ServerStatus
import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.service.model.ClientService
import com.lezenford.mfr.server.service.model.FileService
import com.lezenford.mfr.server.service.model.HistoryService
import com.lezenford.mfr.server.service.model.LauncherService
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.group.ChannelGroup
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.api.tcp.DownloadGameFilesRequest
import ru.fullrest.mfr.common.api.tcp.DownloadLauncherRequest
import ru.fullrest.mfr.common.api.tcp.DownloadResponse
import ru.fullrest.mfr.common.api.tcp.EndSession
import ru.fullrest.mfr.common.api.tcp.Message
import ru.fullrest.mfr.common.api.tcp.ServerMaintenance
import ru.fullrest.mfr.common.exception.ServerMaintenanceException
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.toPath
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ExecutorService

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class RequestInboundHandler(
    private val fileService: FileService,
    private val historyService: HistoryService,
    private val clientService: ClientService,
    private val launcherService: LauncherService,
    private val serverSettingProperties: ServerSettingProperties,
    private val channels: ChannelGroup,
    @Qualifier("historyUpdateExecutor")
    private val historyUpdateExecutor: ExecutorService,
    @Qualifier("uploadFileExecutor")
    private val uploadFileExecutor: ExecutorService
) : SimpleChannelInboundHandler<Message>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        channels.add(ctx.channel())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        when (cause) {
            is ServerMaintenanceException -> {
                ctx.writeAndFlush(ServerMaintenance()).sync()
            }
            else -> log.error(cause)
        }
        ctx.close()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
        when (msg) {
            is DownloadGameFilesRequest -> {
                uploadGameFiles(msg, ctx.channel())
            }
            is DownloadLauncherRequest -> {
                uploadLauncher(msg, ctx.channel())
            }
            else -> {
                log.error("Unsupported type: ${msg::class}")
                ctx.close()
            }
        }
    }

    private fun uploadLauncher(request: DownloadLauncherRequest, channel: Channel) {
        launcherService.findAll().find { it.system == request.systemType }?.also {
            val file = serverSettingProperties.launcherFolder.toPath()
                .resolve(request.systemType.toString())
                .resolve(it.fileName).toFile()
            uploadFileExecutor.submit {
                uploadFiles(listOf(FileInfo(request.id, file)), channel)
                channel.writeAndFlush(EndSession())
            }
        } ?: channel.writeAndFlush(EndSession())
    }

    private fun uploadGameFiles(request: DownloadGameFilesRequest, channel: Channel) {
        val files = request.files.mapNotNull { fileService.findById(it) }.filter { it.active }
        historyUpdateExecutor.execute {
            clientService.updateClientInfo(request.clientId.toString())?.also { client ->
                files.map { it.item }.distinct().forEach { item ->
                    kotlin.runCatching { historyService.updateHistory(item, client) }
                        .onFailure { log.error("Error while update history for item ${item.name}", it) }
                }
            }
        }
        uploadFileExecutor.submit {
            uploadFiles(
                files = files.map {
                    FileInfo(
                        id = it.id,
                        file = Paths.get(serverSettingProperties.build.local)
                            .resolve(it.item.category.build.branch)
                            .resolve(it.path.toPath()).toFile()
                    )
                },
                channel = channel
            )
            channel.writeAndFlush(EndSession())
        }
    }

    private fun uploadFiles(files: List<FileInfo>, channel: Channel) {
        ServerStatus.operation {
            val queue = LinkedList<ChannelFuture>()
            files.forEach { (id, file) ->
                if (channel.isActive) {
                    val length = file.length()
                    RandomAccessFile(file, "r").use { reader ->
                        var availableBytes = length - reader.filePointer
                        do {
                            val buffer = if (availableBytes < BASIC_BUFFER_SIZE) {
                                ByteArray(availableBytes.toInt())
                            } else {
                                ByteArray(BASIC_BUFFER_SIZE)
                            }
                            val startPosition = reader.filePointer
                            reader.read(buffer)
                            availableBytes = length - reader.filePointer
                            if (channel.isWritable.not()) {
                                do {
                                    queue.poll().takeIf { it.isDone.not() }?.sync()
                                } while (channel.isWritable.not())
                            }
                            queue.offer(
                                channel.writeAndFlush(
                                    DownloadResponse(
                                        fileId = id,
                                        position = startPosition,
                                        data = buffer,
                                        lastFrame = availableBytes == 0L
                                    )
                                )
                            )
                        } while (availableBytes > 0)
                    }
                }
            }
            while (queue.isNotEmpty()) {
                queue.poll().takeIf { it.isDone.not() }?.sync()
            }
        }
    }

    private data class FileInfo(
        val id: Int = 0,
        val file: File
    )

    companion object {
        private val log by Logger()
        private const val BASIC_BUFFER_SIZE = 1024 * 64
    }
}