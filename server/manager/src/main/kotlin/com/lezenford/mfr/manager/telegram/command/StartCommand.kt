package com.lezenford.mfr.manager.telegram.command

import com.lezenford.mfr.manager.configuration.properties.TelegramProperties
import com.lezenford.mfr.manager.model.entity.TelegramUser
import com.lezenford.mfr.manager.service.TelegramUserService
import com.lezenford.mfr.manager.telegram.component.element.BotCommand
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class StartCommand(
    private val telegramProperties: TelegramProperties,
    private val telegramUserService: TelegramUserService
) : BotCommand() {
    override val command: String = "start"
    override val permissions: List<TelegramUser.Role> = emptyList()

    override suspend fun execute(message: Message): BotApiMethod<*>? {
        val userId = message.from.id
        val text = runBlocking { telegramUserService.findById(userId) }?.run {
            DENY
        } ?: run {
            val telegramUser = if (userId == telegramProperties.admin) {
                TelegramUser(userId, message.from.userName, TelegramUser.Role.ADMIN)
            } else {
                TelegramUser(userId, message.from.userName, TelegramUser.Role.USER)
            }
            runBlocking { telegramUserService.save(telegramUser) }
            SUCCESS
        }
        return SendMessage(message.chat.id.toString(), text)
    }

    companion object {
        private const val SUCCESS = "Добро пожаловать в консоль сервера проекта MFR. " +
                "Для продолжения работы необходимо повысить уровень доступа, пожалуйста, обратитесь к администратору"
        private const val DENY = "Вы уже зарегистрированы"
    }
}