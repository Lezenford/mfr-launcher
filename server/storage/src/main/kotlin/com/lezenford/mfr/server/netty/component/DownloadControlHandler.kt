package com.lezenford.mfr.server.netty.component

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.*

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class DownloadControlHandler : ChannelOutboundHandlerAdapter() {
    private val messageQueue: Queue<MessageContainer> = LinkedList()
    private var allow = true

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        if (allow) {
            ctx.write(msg, promise)
            sendAvailable()
        } else {
            messageQueue.add(MessageContainer(msg, promise, ctx))
        }
    }

    fun configure(allow: Boolean) {
        this.allow = allow
        if (allow) {
            sendAvailable()
        }
    }

    private fun sendAvailable() {
        while (messageQueue.isNotEmpty()) {
            val container = messageQueue.poll()
            container.ctx.writeAndFlush(container.message, container.promise)
        }
    }

    private data class MessageContainer(
        val message: Any,
        val promise: ChannelPromise,
        val ctx: ChannelHandlerContext
    )
}