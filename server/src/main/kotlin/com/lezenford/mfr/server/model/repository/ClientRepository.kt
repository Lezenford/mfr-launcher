package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.server.model.entity.Client
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface ClientRepository : JpaRepository<Client, Int> {

    fun findByUuid(uuid: String): Client?

    @Modifying
    @Query(
        """
            update Client set lastConnect = current_timestamp where uuid = :uuid
        """
    )
    fun updateTime(uuid: String)

    @Query(
        """
        select count(c) from Client c where c.lastConnect > :date
    """
    )
    fun findClientCountByDate(date: LocalDateTime): Int
}