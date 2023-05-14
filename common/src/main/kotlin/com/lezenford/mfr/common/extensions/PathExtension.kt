package com.lezenford.mfr.common.extensions

import org.apache.commons.io.FilenameUtils
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream

fun Path.md5(): ByteArray {
    val digest = MessageDigest.getInstance("MD5")
    inputStream().use {
        while (it.available() > 0) {
            it.readNBytes(1024 * 1024).also(digest::update)
        }
    }
    return digest.digest()
}

fun String.toPath(root: Path? = null): Path =
    Paths.get(FilenameUtils.separatorsToSystem(root?.resolve(this)?.absolutePathString() ?: this))
