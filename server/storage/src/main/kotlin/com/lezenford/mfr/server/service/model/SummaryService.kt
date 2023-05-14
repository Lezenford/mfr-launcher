package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.common.protocol.enums.ContentType
import com.lezenford.mfr.common.protocol.http.dto.Summary
import com.lezenford.mfr.server.configuration.CacheConfiguration
import com.lezenford.mfr.server.model.entity.History
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SummaryService(
    private val historyService: OverviewService
) {

    @Cacheable(value = [CacheConfiguration.HISTORY_CACHE])
    fun summary(): Summary {
        val lastMonthClients = historyService.findCountByLastUpdate()
        val totalClients = historyService.findCountByLastUpdate(LocalDateTime.of(2021, 1, 1, 0, 0))
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

        return Summary(
            users = Summary.Value(totalClients, lastMonthClients),
            gameDownloads = Summary.Value(mainGameClientCount, mainGameLastMonthClientCount),
            extraContentDownload = Summary.Value(extraGameClientCount, extraGameLastMonthClientCount),
            optionalContentDownload = optionalHistoryMap.mapValues { (_, value) ->
                Summary.Value(
                    value.count(),
                    value.count { it.lastChangeDate.isAfter(LocalDateTime.now().minusMonths(1)) })
            }.toMap()
        )
    }
}