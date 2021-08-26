package ru.fullrest.mfr.common.api.rest

import ru.fullrest.mfr.common.api.SystemType

data class Client(
    val system: SystemType,
    val version: String,
    val md5: ByteArray,
    val size: Long
)