package ru.fullrest.mfr.common.netty

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder
import ru.fullrest.mfr.common.api.tcp.Message

class ResponseEncoder(
    private val objectMapper: ObjectMapper
) : MessageToMessageEncoder<Message>() {
    override fun encode(ctx: ChannelHandlerContext, msg: Message, out: MutableList<Any>) {
        out.add(objectMapper.writeValueAsBytes(msg))
    }
}