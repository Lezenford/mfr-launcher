package com.lezenford.mfr.server.netty.component

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.ifTrue
import com.lezenford.mfr.common.protocol.netty.EndSessionMessage
import com.lezenford.mfr.common.protocol.netty.RequestFileMessage
import com.lezenford.mfr.common.protocol.netty.UploadFileMessage
import com.lezenford.mfr.server.netty.CLIENT_ID
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.group.ChannelGroup
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.*

abstract class DownloadFileHandler<T : RequestFileMessage>(
    private val channelGroup: ChannelGroup
) : SimpleChannelInboundHandler<T>() {
    protected val fileQueue: Queue<FileState> = LinkedList()
    private val buffer = ByteBuffer.allocate(BASIC_BUFFER_SIZE)
    private var lastTask: ChannelFuture? = null

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        channelGroup.add(ctx.channel())
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: T) {
        ctx.channel().attr(CLIENT_ID).set(msg.clientId)
        msg.addToQueue()
        ctx.channel().uploadFiles()
    }

    open fun finished() = fileQueue.isEmpty()

    protected abstract fun T.addToQueue()

    override fun channelInactive(ctx: ChannelHandlerContext) {
        fileQueue.peek()?.close()
    }

    private fun Channel.uploadFiles() {
        fileQueue.peek()?.also {
            if (lastTask?.isDone != false) {
                it.message()?.also { message ->
                    lastTask = writeAndFlush(message).addListener {
                        lastTask = null
                        uploadFiles()
                    }
                } ?: fileQueue.poll().also { uploadFiles() }
            }
        } ?: writeAndFlush(EndSessionMessage())
    }

    protected inner class FileState(
        val id: Int = 0,
        val path: File,
    ) {
        private val channel: FileChannel by lazy { RandomAccessFile(path, "r").channel }
        private val size: Long by lazy { channel.size() }

        fun message(): UploadFileMessage? = channel.takeIf { it.isOpen }?.run {
            val startPosition = channel.position()

            buffer.clear()
            channel.read(buffer)

            UploadFileMessage(fileId = id,
                position = startPosition,
                data = buffer.flip(),
                lastFrame = channel.finished().ifTrue { channel.close() })
        }

        fun close() {
            channel.close()
        }

        private fun FileChannel.finished(): Boolean = size - position() == 0L
    }

    companion object {
        private val log by Logger()
        private const val BASIC_BUFFER_SIZE = 1024 * 64
    }
}