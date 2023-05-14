package com.lezenford.mfr.server.netty.component

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.protocol.netty.ServerExceptionMessage
import com.lezenford.mfr.common.protocol.netty.ServerMaintenanceMessage
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.IOException
import java.util.*
import javax.net.ssl.SSLException


@Suppress("OVERRIDE_DEPRECATION")
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class ChannelExceptionHandler : ChannelDuplexHandler() {

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        when {
            cause.cause is SSLException -> ctx.close() //Error SSL handshake
            cause is IOException -> { //Connection timeout, client disconnect unexpected
                log.info(cause.message)
                ctx.close()
            }
            cause is com.lezenford.mfr.common.exception.ServerMaintenanceException -> ctx.writeAndFlush(
                ServerMaintenanceMessage()
            )
                .addListener { ctx.close() }
            else -> {
                val exceptionUUID = UUID.randomUUID()
                log.error("Error in Server Netty Handler with uuid $exceptionUUID", cause)
                ctx.writeAndFlush(ServerExceptionMessage(uuid = exceptionUUID)).addListener { ctx.close() }
            }
        }
    }

    companion object {
        private val log by Logger()

        const val NAME = "channelExceptionHandler"
    }
}