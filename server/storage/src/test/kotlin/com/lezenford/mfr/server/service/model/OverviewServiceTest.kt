package com.lezenford.mfr.server.service.model

import com.lezenford.mfr.common.protocol.enums.ContentType
import com.lezenford.mfr.server.BaseTest
import com.lezenford.mfr.server.model.entity.Build
import com.lezenford.mfr.server.model.entity.Category
import com.lezenford.mfr.server.model.entity.Client
import com.lezenford.mfr.server.model.entity.Item
import com.lezenford.mfr.server.model.repository.BuildRepository
import com.lezenford.mfr.server.model.repository.CategoryRepository
import com.lezenford.mfr.server.model.repository.ClientRepository
import com.lezenford.mfr.server.model.repository.HistoryRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

internal class OverviewServiceTest : BaseTest() {
    @Autowired
    private lateinit var overviewService: OverviewService

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var buildRepository: BuildRepository

    @Autowired
    private lateinit var clientRepository: ClientRepository

    @Autowired
    private lateinit var historyRepository: HistoryRepository

    @Test
    fun `update history for new client`() {
        val build = Build(name = "test", branch = "test")
        buildRepository.save(build)
        val category = Category(type = ContentType.MAIN, build = build).also { category ->
            category.items.add(
                Item(
                    name = "testItem1",
                    category = category
                )
            )
            category.items.add(
                Item(
                    name = "testItem2",
                    category = category
                )
            )
        }
        categoryRepository.save(category)

        val clientId = UUID.randomUUID().toString()
        overviewService.updateHistory(category.items, clientId)

        val client = clientRepository.findByUuid(clientId)!!
        Assertions.assertThat(historyRepository.findByItemsAndClient(category.items, client)).hasSize(2)
    }

    @Test
    fun `update history for exist client`() {
        val build = Build(name = "test", branch = "test")
        buildRepository.save(build)
        val category = Category(type = ContentType.MAIN, build = build).also { category ->
            category.items.add(
                Item(
                    name = "testItem1",
                    category = category
                )
            )
            category.items.add(
                Item(
                    name = "testItem2",
                    category = category
                )
            )
        }
        categoryRepository.save(category)

        val client = Client(uuid = UUID.randomUUID().toString())
        clientRepository.save(client)

        overviewService.updateHistory(category.items, client.uuid)

        Assertions.assertThat(historyRepository.findByItemsAndClient(category.items, client)).hasSize(2)
    }
}