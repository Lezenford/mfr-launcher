package ru.fullrest.mfr.common.api.rest

import ru.fullrest.mfr.common.api.ReportType

data class ReportDto(
    val type: ReportType,

    val text: String
)