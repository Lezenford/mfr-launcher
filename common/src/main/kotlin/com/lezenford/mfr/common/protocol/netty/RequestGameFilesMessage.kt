package com.lezenford.mfr.common.protocol.netty

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class RequestGameFilesMessage(
    @JsonProperty("id")
    override val clientId: UUID,
    @JsonProperty("f")
    val files: List<Int>
) : RequestFileMessage()