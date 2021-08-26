package ru.fullrest.mfr.common.api.tcp

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty

data class DownloadResponse(
    @JsonProperty("id")
    val fileId: Int,

    @JsonProperty("p")
    val position: Long,

    @JsonProperty("d")
    val data: ByteArray,

    @JsonProperty("l")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    val lastFrame: Boolean = false
) : Message()