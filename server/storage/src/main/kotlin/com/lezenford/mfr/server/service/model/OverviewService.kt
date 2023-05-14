package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.server.model.entity.Client
import com.lezenford.mfr.server.model.entity.History
import com.lezenford.mfr.server.model.entity.Item
import com.lezenford.mfr.server.model.repository.ClientRepository
import com.lezenford.mfr.server.model.repository.HistoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OverviewService(
    private val historyRepository: HistoryRepository,
    private val clientRepository: ClientRepository
) {
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = [])
    fun updateHistory(items: List<Item>, clientId: String) {
        val client = clientRepository.findByUuid(clientId)?.also {
            clientRepository.updateLastConnectionTime(clientId)
        } ?: clientRepository.save(Client(uuid = clientId))
        val existingHistory = historyRepository.findByItemsAndClient(items, client)
        historyRepository.updateTime(existingHistory.map { it.id })

        val existingItemsIds = existingHistory.map { it.item.id }
        items.filterNot { existingItemsIds.contains(it.id) }.takeIf { it.isNotEmpty() }
            ?.map { History(client = client, item = it) }?.also {
                historyRepository.saveAll(it)
            }
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = [])
    fun findAll(): List<History> = historyRepository.findAll()

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = [])
    fun updateClientInfo(uuid: String) {
        return clientRepository.updateLastConnectionTime(uuid) ifNotExist {
            clientRepository.save(Client(uuid = uuid))
        }
    }

    fun findByUuid(uuid: String): Client? = clientRepository.findByUuid(uuid)

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = [])
    fun findCountByLastUpdate(localDateTime: LocalDateTime = LocalDateTime.now().minusMonths(1)): Int =
        clientRepository.findClientCountByDate(localDateTime)

    private infix fun Int.ifNotExist(func: () -> Unit) {
        if (this == 0) {
            func()
        }
    }

    companion object {
        private val log by Logger()
    }
}