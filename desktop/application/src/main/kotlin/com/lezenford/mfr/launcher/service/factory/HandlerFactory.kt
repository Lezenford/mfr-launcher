package com.lezenford.mfr.launcher.service.factory

import com.fasterxml.jackson.databind.ObjectMapper
import com.lezenford.mfr.common.netty.RequestDecoder
import com.lezenford.mfr.common.netty.ResponseEncoder
import com.lezenford.mfr.launcher.netty.ExceptionHandler
import com.lezenford.mfr.launcher.netty.InboundFileHandler
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
    fun inboundHandler(): InboundFileHandler = lookup()

    @Lookup
    fun exceptionHandler(): ExceptionHandler = lookup()

    @Lookup
    fun channelTrafficShapingHandler(): AbstractTrafficShapingHandler = lookup()

    private inline fun <reified T> lookup(): T = Any() as T
}