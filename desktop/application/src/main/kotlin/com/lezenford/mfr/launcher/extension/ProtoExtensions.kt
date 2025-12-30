package com.lezenford.mfr.launcher.extension

import com.google.protobuf.GeneratedMessageV3
import java.io.ByteArrayInputStream
import java.security.MessageDigest

fun GeneratedMessageV3.sha256(): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256")
    ByteArrayInputStream(toByteArray()).use {
        while (it.available() > 0) {
            it.readNBytes(1024 * 1024).also(digest::update)
        }
    }
    return digest.digest()
}