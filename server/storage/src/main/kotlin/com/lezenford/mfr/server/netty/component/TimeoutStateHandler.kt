package com.lezenford.mfr.server.netty.component

import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class TimeoutStateHandler(
    private val properties: ServerSettingProperties
) : ChannelOutboundHandlerAdapter() {
    private var establish = false

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        ctx.executor().schedule({ if (establish.not()) ctx.close() }, properties.netty.timeout, TimeUnit.SECONDS)
    }

    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
        establish = true
        ctx.pipeline().remove(this)
        ctx.write(msg, promise)
    }
}