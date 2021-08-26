package ru.fullrest.mfr.common.api.rest

import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDateTime

data class BuildDto(
    val id: Int,
    val name: String,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    val lastUpdate: LocalDateTime
)