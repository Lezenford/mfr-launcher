package com.lezenford.mfr.common.protocol.http.dto

import com.lezenford.mfr.common.protocol.enums.SystemType

data class LauncherDto(
    val type: SystemType,
    val version: String? = null,
    val fileName: String? = null
)