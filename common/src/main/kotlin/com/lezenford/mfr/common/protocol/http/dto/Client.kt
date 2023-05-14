package com.lezenford.mfr.common.protocol.http.dto

import com.lezenford.mfr.common.protocol.enums.SystemType

data class Client(
    val system: SystemType,
    val version: String,
    val md5: ByteArray,
    val size: Long
)