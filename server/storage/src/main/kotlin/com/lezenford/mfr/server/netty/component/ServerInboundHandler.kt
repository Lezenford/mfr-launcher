package com.lezenford.mfr.server.netty.component

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.netty.SimpleChannelOutboundHandler
import com.lezenford.mfr.common.protocol.netty.*
import com.lezenford.mfr.server.netty.HandlerFactory
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.group.ChannelGroup
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class ServerInboundHandler(
    private val clients: ChannelGroup,
    private val factory: HandlerFactory
) : SimpleChannelInboundHandler<Message>(), SimpleChannelOutboundHandler {
    private val downloadControlHandler = factory.downloadControlHandler()
    private val gameDownloadFileHandler: GameDownloadFileHandler by lazy { factory.gameDownloadFileHandler() }
    private val launcherDownloadFileHandler: LauncherDownloadFileHandler by lazy { factory.launcherDownloadFileHandler() }

    override fun channelActive(ctx: ChannelHandlerContext) {
        clients.add(ctx.channel())
        ctx.pipeline().addBefore(ChannelExceptionHandler.NAME, null, downloadControlHandler)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
        when (msg) {
            is RequestGameFilesMessage -> {
                ctx.pipeline().get(GameDownloadFileHandler::class.java)
                    ?: ctx.pipeline().addBefore(ChannelExceptionHandler.NAME, null, gameDownloadFileHandler)
                ctx.fireChannelRead(msg)
            }

            is RequestLauncherFilesMessage -> {
                ctx.pipeline().get(LauncherDownloadFileHandler::class.java)
                    ?: ctx.pipeline().addBefore(ChannelExceptionHandler.NAME, null, launcherDownloadFileHandler)
                ctx.fireChannelRead(msg)
            }

            is RequestChangeState -> {
                downloadControlHandler.configure(msg.active)
            }
            else -> throw UnsupportedOperationException("Unsupported incoming message type: ${msg::class}")
        }
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        if (msg is EndSessionMessage) {
            if (gameDownloadFileHandler.finished() && launcherDownloadFileHandler.finished()) {
                ctx.write(msg, promise).addListener { ctx.close() }
            }
        } else {
            ctx.write(msg, promise)
        }
    }

    companion object {
        private val log by Logger()
    }
}