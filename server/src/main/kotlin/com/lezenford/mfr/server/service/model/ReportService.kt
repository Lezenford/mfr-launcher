package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.model.entity.Report
import com.lezenford.mfr.server.model.repository.ReportRepository
import org.springframework.stereotype.Service
import ru.fullrest.mfr.common.api.ReportType
import ru.fullrest.mfr.common.api.rest.ReportDto
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val clientService: ClientService
) {

    fun find(date: LocalDateTime, type: ReportType? = null, clientUuid: String? = null): List<Report> {
        return reportRepository.findLastByDate(date)
    }

    @Transactional
    fun create(report: ReportDto, identity: String) {
        val client = clientService.findByUuid(identity)
            ?: throw IllegalArgumentException("Client $identity not found")

        reportRepository.save(
            Report(
                client = client,
                type = report.type,
                text = report.text
            )
        )
    }
}