package com.lezenford.mfr.common.netty

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import com.lezenford.mfr.common.protocol.netty.Message
import java.io.InputStream

class RequestDecoder(
    private val objectMapper: ObjectMapper
) : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, `in`: ByteBuf, out: MutableList<Any>) {
        ByteBufInputStream(`in`).use { inputStream: InputStream ->
            out.add(objectMapper.readValue(inputStream, Message::class.java))
        }
    }
}