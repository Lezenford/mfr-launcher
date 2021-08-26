package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.server.configuration.CacheConfiguration
import com.lezenford.mfr.server.model.entity.History
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import ru.fullrest.mfr.common.api.ContentType
import java.time.LocalDateTime

@Service
class StatisticService(
    private val clientService: ClientService,
    private val historyService: HistoryService
) {

    @Cacheable(value = [CacheConfiguration.HISTORY_CACHE])
    fun statistic(): String {
        val lastMonthClients = clientService.findCountByLastUpdate()
        val totalClients = clientService.findCountByLastUpdate(LocalDateTime.of(2021, 1, 1, 0, 0))
        val history = historyService.findAll()
        val historyMain = mutableListOf<History>()
        val historyExtra = mutableListOf<History>()
        val historyOptional = mutableListOf<History>()
        history.forEach {
            when (it.item.category.type) {
                ContentType.MAIN -> historyMain.add(it)
                ContentType.EXTRA -> historyExtra.add(it)
                ContentType.OPTIONAL -> historyOptional.add(it)
            }
        }
        val mainGameClientCount = historyMain.distinctBy { it.client.id }.size
        val mainGameLastMonthClientCount =
            historyMain.filter { it.lastChangeDate.isAfter(LocalDateTime.now().minusMonths(1)) }
                .distinctBy { it.client.id }.size
        val extraGameClientCount = historyExtra.distinctBy { it.client.id }.size
        val extraGameLastMonthClientCount =
            historyExtra.filter { it.lastChangeDate.isAfter(LocalDateTime.now().minusMonths(1)) }
                .distinctBy { it.client.id }.size


        val optionalHistoryMap = historyOptional.map { it.item.name to it }.let {
            val result = mutableMapOf<String, MutableList<History>>()
            it.forEach { (name, history) ->
                result.getOrPut(name) { mutableListOf() }.add(history)
            }
            result
        }

        return """Всего уникальных пользователей: $totalClients 
        |Уникальных пользователей за последний месяц $lastMonthClients 
        
        |Скачивали игру: $mainGameClientCount 
        |Скачивали игру за последний месяц: $mainGameLastMonthClientCount 
        
        |Скачивали дополнительный контент: $extraGameClientCount 
        |Скачивали дополнительный контент за последний месяц: $extraGameLastMonthClientCount 
        
        |Скачивали опциональный контент: ${
            optionalHistoryMap.map { (key, value) ->
                "$key: ${value.count()}"
            }.joinToString("\n")
        } 
        
        |Скачивали опциональный контент за последний месяц: ${
            optionalHistoryMap.map { (key, value) ->
                "$key: ${value.count { it.lastChangeDate.isAfter(LocalDateTime.now().minusMonths(1)) }}"
            }.joinToString("\n")
        }""".trimMargin()
    }
}