package com.lezenford.mfr.server.netty.component

import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.service.model.ClientService
import com.lezenford.mfr.server.service.model.FileService
import com.lezenford.mfr.server.service.model.HistoryService
import com.lezenford.mfr.server.service.model.LauncherService
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.group.ChannelGroup
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.api.tcp.DownloadLauncherRequest
import ru.fullrest.mfr.common.api.tcp.DownloadRequest
import ru.fullrest.mfr.common.api.tcp.DownloadResponse
import ru.fullrest.mfr.common.api.tcp.EndSession
import ru.fullrest.mfr.common.api.tcp.Message
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.toPath
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantReadWriteLock

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class RequestInboundHandler(
    private val fileService: FileService,
    private val historyService: HistoryService,
    private val clientService: ClientService,
    private val launcherService: LauncherService,
    private val serverSettingProperties: ServerSettingProperties,
    private val channels: ChannelGroup,
    private val serverGlobalFileLock: ReentrantReadWriteLock,
    private val historyUpdateExecutor: ExecutorService
) : SimpleChannelInboundHandler<Message>() {
    private val executor = Executors.newSingleThreadExecutor()
    private var currentTask: Future<*>? = null

    override fun channelActive(ctx: ChannelHandlerContext) {
        channels.add(ctx.channel())
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        executor.shutdownNow()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        log.error(cause)
        ctx.close()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
        when (msg) {
            is DownloadRequest -> {
                uploadFiles(msg, ctx.channel())
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
            val file = serverSettingProperties.launcherFolder.toPath().resolve(request.systemType.toString())
                .resolve(it.fileName)
            currentTask = executor.submit {
                uploadFile(0, file.toFile(), channel)
                channel.writeAndFlush(EndSession())
            }
        } ?: channel.writeAndFlush(EndSession())
    }

    private fun uploadFiles(request: DownloadRequest, channel: Channel) {
        if (currentTask == null || currentTask?.isDone != false || currentTask?.isCancelled != false) {
            val files = request.files.mapNotNull { fileService.findById(it) }.filter { it.active }
            historyUpdateExecutor.execute {
                clientService.updateClientInfo(request.clientId.toString())?.also { client ->
                    files.map { it.item }.distinct().forEach { item ->
                        kotlin.runCatching { historyService.updateHistory(item, client) }
                            .onFailure { log.error("Error while update history for item ${item.name}", it) }
                    }
                }
            }
            currentTask = executor.submit {
                files.forEach { fileInfo ->
                    if (channel.isActive) {
                        val file = Paths.get(serverSettingProperties.buildFolder)
                            .resolve(fileInfo.item.category.build.branch)
                            .resolve(fileInfo.path.toPath()).toFile()
                        uploadFile(fileInfo.id, file, channel)
                    }
                }
                channel.writeAndFlush(EndSession())
            }
        } else {
            log.error("Client task already run")
        }
    }

    private fun uploadFile(id: Int, file: File, channel: Channel) {
        if (serverGlobalFileLock.readLock().tryLock()) {
            try {
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
                        val response = DownloadResponse(id, startPosition, buffer, availableBytes == 0L)
                        channel.writeAndFlush(response).sync()
                    } while (availableBytes > 0)
                }
            } finally {
                serverGlobalFileLock.readLock().unlock()
            }
        }
    }

    companion object {
        private val log by Logger()
        const val BASIC_BUFFER_SIZE = 1024 * 512
    }
}