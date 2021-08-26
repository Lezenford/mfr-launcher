package ru.fullrest.mfr.common.api.tcp

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class DownloadRequest(
    @JsonProperty("id")
    val clientId: UUID,
    @JsonProperty("f")
    val files: List<Int>
) : Message()