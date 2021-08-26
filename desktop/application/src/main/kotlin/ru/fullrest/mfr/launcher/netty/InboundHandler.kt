package ru.fullrest.mfr.launcher.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import ru.fullrest.mfr.common.api.SystemType
import ru.fullrest.mfr.common.api.rest.Content
import ru.fullrest.mfr.common.api.tcp.DownloadLauncherRequest
import ru.fullrest.mfr.common.api.tcp.DownloadRequest
import ru.fullrest.mfr.common.api.tcp.DownloadResponse
import ru.fullrest.mfr.common.api.tcp.EndSession
import ru.fullrest.mfr.common.api.tcp.Message
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.toPath
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import kotlin.io.path.exists

class InboundHandler(
    private val requestFiles: MutableMap<Int, Content.Category.Item.File>,
    private val clientId: UUID,
    private val targetFolder: Path,
    private val progressObserver: (Pair<Long, Long>) -> Unit
) : SimpleChannelInboundHandler<Message>() {
    private val md5Map = mutableMapOf<Int, MessageDigest>()
    private val totalSize = requestFiles.values.sumOf { it.size }
    private var downloadedSize = 0L

    override fun channelActive(ctx: ChannelHandlerContext) {
        //TODO поменять, сделать скачивание более универсальным
        if (requestFiles.keys.contains(0)) {
            ctx.writeAndFlush(DownloadLauncherRequest(systemType = SystemType.WINDOWS))
        } else {
            ctx.writeAndFlush(DownloadRequest(clientId = clientId, files = requestFiles.keys.toList()))
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
        when (msg) {
            is EndSession -> ctx.close()
            is DownloadResponse -> writeFile(msg)
            else -> log.warn("Unsupported message type: ${msg::class}")
        }
    }

    private fun writeFile(msg: DownloadResponse) {
        val file = requestFiles[msg.fileId]
            ?: throw IllegalArgumentException("File queue doesn't contain file with id ${msg.fileId}")

        val path = targetFolder.resolve(file.path.toPath())
        if (path.parent.exists().not()) {
            Files.createDirectories(path.parent)
        }
        RandomAccessFile(path.toFile(), "rw").use {
            it.seek(msg.position)
            it.write(msg.data)
        }
        downloadedSize += msg.data.size
        progressObserver(downloadedSize to totalSize)
        md5Map.getOrPut(file.id) { MessageDigest.getInstance("MD5") }.apply {
            update(msg.data)
            if (msg.lastFrame) {
                if (digest().contentEquals(file.md5)) {
                    requestFiles.remove(msg.fileId)
                } else {
                    log.error("File ${file.path} has incorrect MD5 sum")
                }
            }
        }

    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        log.error("Netty error", cause)
    }

    companion object {
        private val log by Logger()
    }
}