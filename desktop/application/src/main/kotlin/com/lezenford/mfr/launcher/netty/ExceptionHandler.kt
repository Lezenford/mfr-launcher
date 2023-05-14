package com.lezenford.mfr.launcher.netty

import com.lezenford.mfr.common.extensions.Logger
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Suppress("OVERRIDE_DEPRECATION")
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class ExceptionHandler : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        ReferenceCountUtil.release(msg)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        log.error("Netty client throw exception", cause)
        ctx.close()
    }

    companion object {
        private val log by Logger()
    }
}