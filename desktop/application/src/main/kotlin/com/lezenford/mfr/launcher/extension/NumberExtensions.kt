package com.lezenford.mfr.launcher.extension

import com.lezenford.mfr.common.extensions.format

fun Long.toTraffic(): String {
    return if (this > 1024) {
        val kilobytes = this / 1024.0
        if (kilobytes > 1024.0) {
            val megabytes = kilobytes / 1024.0
            "${megabytes.format(2)} МБ/с"
        } else {
            "${kilobytes.format(2)} КБ/с"
        }
    } else {
        "$this Б/с"
    }
}