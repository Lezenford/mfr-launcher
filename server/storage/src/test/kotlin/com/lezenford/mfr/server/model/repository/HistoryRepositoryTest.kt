package com.lezenford.mfr.server.model.repository

import com.lezenford.mfr.common.protocol.enums.ContentType
import com.lezenford.mfr.server.BaseTest
import com.lezenford.mfr.server.model.entity.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class HistoryRepositoryTest : BaseTest() {

    @Autowired
    private lateinit var clientRepository: ClientRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var buildRepository: BuildRepository

    @Autowired
    private lateinit var historyRepository: HistoryRepository

    @Test
    fun `find history by client and items list`() {
        val client = Client()
        clientRepository.save(client)

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

        historyRepository.save(History(client = client, item = category.items.first()))
        historyRepository.save(History(client = client, item = category.items.last()))

        val historyList = historyRepository.findByItemsAndClient(category.items, client)

        Assertions.assertThat(historyList).hasSize(2)
    }
}