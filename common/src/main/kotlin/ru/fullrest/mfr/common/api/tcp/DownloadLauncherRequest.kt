package ru.fullrest.mfr.common.api.tcp

import com.fasterxml.jackson.annotation.JsonProperty
import ru.fullrest.mfr.common.api.SystemType

data class DownloadLauncherRequest(
    val id: Int = 0,

    @JsonProperty("type")
    val systemType: SystemType
) : Message()