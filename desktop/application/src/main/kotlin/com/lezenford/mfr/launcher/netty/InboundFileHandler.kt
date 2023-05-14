package com.lezenford.mfr.launcher.netty

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.protocol.netty.*
import com.lezenford.mfr.launcher.exception.DownloadFileException
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.io.path.exists

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class InboundFileHandler(
    private val nettyProgressFlow: MutableSharedFlow<Long>
) : SimpleChannelInboundHandler<Message>() {
    private val files = mutableMapOf<Int, FileContent>()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
        when (msg) {
            is EndSessionMessage -> ctx.close()
            is UploadFileMessage -> msg.writeFile(ctx)
            is ServerMaintenanceMessage -> {}
            is ServerExceptionMessage -> {}
            else -> log.warn("Unsupported message type: ${msg::class}")
        }
    }

    private fun UploadFileMessage.writeFile(ctx: ChannelHandlerContext) {
        val content: FileContent = files.getOrPut(fileId) {
            ctx.channel().attr(REQUESTED_FILES).get()?.let { files ->
                files.find { it.id == fileId }?.let { FileContent(it) }
                    ?: throw DownloadFileException("File with id $fileId not found in attribute list")
            } ?: throw DownloadFileException("Attribute with file list not found")
        }

        data.mark()

        content.channel.position(position)
        content.channel.write(data)

        content.md5.update(data.reset())

        val dataSize = data.limit().toLong()
        CoroutineScope(Dispatchers.IO).launch {
            nettyProgressFlow.emit(dataSize)
        }

        if (lastFrame) {
            content.channel.close()
            files.remove(fileId)
            if (content.md5.digest().contentEquals(content.file.md5).not()) {
                log.error("File ${content.file.path} has incorrect MD5 sum")
            }
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        files.values.forEach { it.channel.close() }
        files.clear()
    }

    private data class FileContent(
        val file: FileData
    ) {
        val channel: FileChannel = kotlin.run {
            if (file.path.parent.exists().not()) {
                Files.createDirectories(file.path.parent)
            }
            RandomAccessFile(file.path.toFile(), "rw").channel
        }
        val md5: MessageDigest = MessageDigest.getInstance("MD5")
    }

    companion object {
        private val log by Logger()
    }
}