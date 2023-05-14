package com.lezenford.mfr.launcher.netty

import io.netty.util.AttributeKey
import java.nio.file.Path

val REQUESTED_FILES: AttributeKey<List<FileData>> = AttributeKey.valueOf("RequestedFiles")

data class FileData(
    val id: Int,
    val path: Path,
    val size: Long,
    val md5: ByteArray
)