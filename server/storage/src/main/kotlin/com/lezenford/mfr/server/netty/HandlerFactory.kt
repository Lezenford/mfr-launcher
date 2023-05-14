package com.lezenford.mfr.server.netty

import com.fasterxml.jackson.databind.ObjectMapper
import com.lezenford.mfr.common.netty.RequestDecoder
import com.lezenford.mfr.common.netty.ResponseEncoder
import com.lezenford.mfr.server.netty.component.*
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.traffic.AbstractTrafficShapingHandler
import org.springframework.beans.factory.annotation.Lookup
import org.springframework.stereotype.Component

@Component
class HandlerFactory(
    private val objectMapper: ObjectMapper
) {
    fun lengthFieldBasedFrameDecoder(): LengthFieldBasedFrameDecoder =
        LengthFieldBasedFrameDecoder(1024 * 1024, 0, 3, 0, 3)

    fun lengthFieldPrepender(): LengthFieldPrepender = LengthFieldPrepender(3)

    fun requestDecoder(): RequestDecoder = RequestDecoder(objectMapper)

    fun responseEncoder(): ResponseEncoder = ResponseEncoder(objectMapper)

    @Lookup
    fun timeoutStateHandler(): TimeoutStateHandler = lookup()

    @Lookup
    fun serverInboundHandler(): ServerInboundHandler = lookup()

    @Lookup
    fun gameDownloadFileHandler(): GameDownloadFileHandler = lookup()

    @Lookup
    fun launcherDownloadFileHandler(): LauncherDownloadFileHandler = lookup()

    @Lookup
    fun downloadControlHandler(): DownloadControlHandler = lookup()

    @Lookup
    fun trafficShapingHandler(): AbstractTrafficShapingHandler = lookup()

    @Lookup
    fun channelExceptionHandler(): ChannelExceptionHandler = lookup()

    private inline fun <reified T> lookup(): T = Any() as T
}