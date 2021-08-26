package com.lezenford.mfr.server.telegram.command

import com.lezenford.mfr.server.configuration.properties.TelegramProperties
import com.lezenford.mfr.server.model.entity.TelegramUser
import com.lezenford.mfr.server.service.model.TelegramUserService
import com.lezenford.mfr.server.telegram.component.BotCommand
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

    override fun execute(message: Message): BotApiMethod<*>? {
        val userId = message.from.id
        val text = telegramUserService.findById(userId)?.run {
            DENY
        } ?: run {
            val telegramUser = if (userId == telegramProperties.admin) {
                TelegramUser(userId, message.from.userName, TelegramUser.Role.ADMIN)
            } else {
                TelegramUser(userId, message.from.userName, TelegramUser.Role.USER)
            }
            telegramUserService.save(telegramUser)
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