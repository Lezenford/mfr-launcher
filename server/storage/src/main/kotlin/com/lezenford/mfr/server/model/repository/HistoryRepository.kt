package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.server.model.entity.Client
import com.lezenford.mfr.server.model.entity.History
import com.lezenford.mfr.server.model.entity.Item
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface HistoryRepository : JpaRepository<History, Int> {

    @Query(
        """
        select h from History h where h.client = :client and h.item in :items
    """
    )
    fun findByItemsAndClient(items: List<Item>, client: Client): List<History>

    @Modifying
    @Query(
        """
            update History h set h.lastChangeDate = current_timestamp where h.id in :ids
        """
    )
    fun updateTime(ids: List<Int>)
}