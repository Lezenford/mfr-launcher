package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.model.entity.Client
import com.lezenford.mfr.server.model.entity.History
import com.lezenford.mfr.server.model.entity.Item
import com.lezenford.mfr.server.model.repository.HistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional

@Service
class HistoryService(
    private val historyRepository: HistoryRepository
) {

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = [])
    fun updateHistory(item: Item, client: Client) {
        historyRepository.findByItemAndClient(item, client)?.also { historyRepository.updateTime(it.id) }
            ?: historyRepository.save(History(item = item, client = client))
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = [])
    fun findAll(): List<History> = historyRepository.findAll()
}