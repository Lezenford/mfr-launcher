package com.lezenford.mfr.manager.telegram.command

import com.lezenford.mfr.manager.model.entity.TelegramUser
import com.lezenford.mfr.manager.service.TelegramUserService
import com.lezenford.mfr.manager.telegram.component.element.BotCommand
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class ExitBotCommand(val telegramUserService: TelegramUserService) : BotCommand() {
    override val command: String = "exit"
    override val permissions: List<TelegramUser.Role> = listOf(TelegramUser.Role.USER, TelegramUser.Role.ADMIN)

    override suspend fun execute(message: Message): BotApiMethod<*>? {
        telegramUserService.deleteById(message.from.id)
        return SendMessage().apply {
            chatId = message.chatId.toString()
            text = "Ваш аккаунт успешно удален"
        }
    }
}