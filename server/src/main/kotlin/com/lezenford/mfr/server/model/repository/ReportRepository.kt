package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.server.model.entity.Report
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.fullrest.mfr.common.api.ReportType
import java.time.LocalDateTime
import java.util.*

interface ReportRepository : JpaRepository<Report, Long> {

    @Query(
        """
            select r from Report r 
            where r.uploadDateTime > :date
            order by r.uploadDateTime
        """
    )
    fun findLastByDate(date: LocalDateTime): List<Report>

    @Query(
        """
            select r from Report r
            where  r.uploadDateTime > :date and r.type = :type 
            order by r.uploadDateTime
        """
    )
    fun findLastByDateAndType(date: LocalDateTime, type: ReportType): List<Report>

    @Query(
        """
            select r from Report r
            join fetch r.client c
            where r.uploadDateTime > :date and r.type = :type and  c.uuid = :clientUuid
            order by r.type, r.uploadDateTime
        """
    )
    fun findLastByDateAndTypeAndClient(date: LocalDateTime, type: ReportType, clientUuid: UUID): List<Report>
}