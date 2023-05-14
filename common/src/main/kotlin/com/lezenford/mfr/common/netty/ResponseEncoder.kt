package com.lezenford.mfr.common.netty

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufOutputStream
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import com.lezenford.mfr.common.protocol.netty.Message
import java.io.OutputStream

class ResponseEncoder(
    private val objectMapper: ObjectMapper
) : MessageToByteEncoder<Message>() {
    override fun encode(ctx: ChannelHandlerContext, msg: Message, out: ByteBuf) {
        ByteBufOutputStream(out).use { outputStream: OutputStream ->
            objectMapper.writeValue(outputStream, msg)
        }
    }
}