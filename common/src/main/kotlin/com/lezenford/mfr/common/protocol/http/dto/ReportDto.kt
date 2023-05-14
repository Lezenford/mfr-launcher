package com.lezenford.mfr.common.protocol.http.dto

import com.lezenford.mfr.common.protocol.enums.ReportType

data class ReportDto(
    val type: ReportType,

    val text: String
)