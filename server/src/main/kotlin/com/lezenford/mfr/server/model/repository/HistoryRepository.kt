package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.server.model.entity.Client
import com.lezenford.mfr.server.model.entity.History
import com.lezenford.mfr.server.model.entity.Item
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface HistoryRepository : JpaRepository<History, Int> {

    fun findByItemAndClient(item: Item, client: Client): History?

    @Modifying
    @Query(
        """
            update History set lastChangeDate = current_timestamp where id = :id
        """
    )
    fun updateTime(id: Int)
}