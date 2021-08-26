package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.model.entity.Client
import com.lezenford.mfr.server.model.repository.ClientRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ClientService(
    private val clientRepository: ClientRepository
) {
    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = [])
    fun updateClientInfo(uuid: String): Client? {
        return clientRepository.findByUuid(uuid)?.also { clientRepository.updateTime(uuid) }
            ?: clientRepository.save(Client(uuid = uuid))
    }

    fun findByUuid(uuid: String): Client? = clientRepository.findByUuid(uuid)

    @Transactional(isolation = Isolation.READ_UNCOMMITTED, rollbackFor = [])
    fun findCountByLastUpdate(localDateTime: LocalDateTime = LocalDateTime.now().minusMonths(1)): Int =
        clientRepository.findClientCountByDate(localDateTime)
}