package ru.fullrest.mfr.common.netty

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import ru.fullrest.mfr.common.api.tcp.Message

class RequestDecoder(
    private val objectMapper: ObjectMapper
) : MessageToMessageDecoder<ByteArray>() {
    override fun decode(ctx: ChannelHandlerContext, msg: ByteArray, out: MutableList<Any>) {
        out.add(objectMapper.readValue<Message>(msg))
    }
}