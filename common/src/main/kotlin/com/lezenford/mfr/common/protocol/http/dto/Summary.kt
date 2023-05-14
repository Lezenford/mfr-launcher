package com.lezenford.mfr.common.protocol.http.dto

data class Summary(
    val users: Value,
    val gameDownloads: Value,
    val extraContentDownload: Value,
    val optionalContentDownload: Map<String, Value>
) {
    data class Value(
        val total: Int,
        val lastMonth: Int
    )
}