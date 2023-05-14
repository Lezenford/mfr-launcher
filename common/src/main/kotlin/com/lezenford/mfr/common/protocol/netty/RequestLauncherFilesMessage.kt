package com.lezenford.mfr.common.protocol.netty

import com.fasterxml.jackson.annotation.JsonProperty
import com.lezenford.mfr.common.protocol.enums.SystemType
import java.util.*

data class RequestLauncherFilesMessage(
    @JsonProperty("id")
    override val clientId: UUID = UUID.randomUUID(),
    @JsonProperty("type")
    val systemType: SystemType
) : RequestFileMessage()