@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lezenford.mfr.manager.telegram.menu

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lezenford.mfr.manager.BaseTest
import com.lezenford.mfr.manager.configuration.properties.TelegramProperties
import com.lezenford.mfr.manager.extensions.convert
import com.lezenford.mfr.manager.model.entity.TelegramUser
import com.lezenford.mfr.manager.model.repository.TelegramUserRepository
import com.lezenford.mfr.manager.service.StorageService
import com.lezenford.mfr.manager.telegram.command.UsersCommand
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Chat
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.MessageEntity
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

@Disabled
internal class UserRoleMenuTest : BaseTest() {
    @SpyBean
    private lateinit var storageService: StorageService

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var userRoleMenu: UserRoleMenu

    @Autowired
    private lateinit var telegramUserRepository: TelegramUserRepository

    @Autowired
    private lateinit var telegramProperties: TelegramProperties

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun prepareDb() = runTest {
        telegramUserRepository.deleteAll()
        telegramUserRepository.save(user)
        telegramUserRepository.save(admin)
    }

    @BeforeEach
    fun setUp() = runTest {
        Mockito.reset(storageService)
    }

    @AfterEach
    fun cleanDb() = runTest {
        runBlocking { telegramUserRepository.deleteAll() }
    }

    @Test
    fun `welcome message test`() = runTest {
        val update = Update().apply {
            message = messageTemplate.apply {
                text = "/${UsersCommand.command}"
                entities.add(MessageEntity().also {
                    it.type = "bot_command"
                    it.offset = 0
                    it.length = text.length
                    it.text = text
                })
                from = User().apply {
                    id = admin.telegramId
                }
                chat = Chat().apply {
                    id = admin.telegramId
                }
            }
        }

        sendMessage(update).expectStatus().is2xxSuccessful.expectBody<Message>().returnResult().responseBody!!.also {
                Assertions.assertThat(it.text).isEqualTo(userRoleMenu.firstMenuItem.await().text.await())
            }
    }

    @Test
    fun `check second message`() = runTest {
        val menuItem = userRoleMenu.firstMenuItem.await().buttons.first { it.text == user.username }.menuItem
        val update = Update().apply {
            callbackQuery = CallbackQuery().apply {
                id = "1"
                message = Message().apply {
                    id = "1"
                    chat = Chat().apply { id = admin.telegramId }
                }
                from = User().apply {
                    id = admin.telegramId
                }
                data = menuItem.identity.convert()
            }
        }

        sendMessage(update).expectStatus().is2xxSuccessful.expectBody<Message>().returnResult().responseBody!!.also {
                Assertions.assertThat(it.text).isEqualTo(menuItem.text.await())
            }
    }

    private val messageTemplate
        get() = objectMapper.readValue<Message>("{\"entities\":[]}")

    private fun sendMessage(update: Update) =
        webClient.post().uri("/telegram/${telegramProperties.token}").bodyValue(update).exchange()

    companion object {
        private val user = TelegramUser(1, "user", TelegramUser.Role.USER)
        private val admin = TelegramUser(2, "admin", TelegramUser.Role.ADMIN)
    }
}