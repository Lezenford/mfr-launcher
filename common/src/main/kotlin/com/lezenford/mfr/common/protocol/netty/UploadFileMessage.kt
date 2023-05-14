package com.lezenford.mfr.common.protocol.netty

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.nio.ByteBuffer

data class UploadFileMessage(
    @JsonProperty("id")
    val fileId: Int,

    @JsonProperty("p")
    val position: Long,

    @JsonProperty("d")
    val data: ByteBuffer = EMPTY_ARRAY,

    @JsonProperty("l")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val lastFrame: Boolean = false
) : Message() {
    companion object {
        private val EMPTY_ARRAY = ByteBuffer.allocate(0)
    }
}