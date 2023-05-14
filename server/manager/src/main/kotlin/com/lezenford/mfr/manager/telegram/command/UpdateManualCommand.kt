package com.lezenford.mfr.manager.telegram.command

import com.lezenford.mfr.manager.model.entity.TelegramUser
import com.lezenford.mfr.manager.service.StorageService
import com.lezenford.mfr.manager.telegram.component.element.BotCommand
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class UpdateManualCommand(
    private val storageService: StorageService
) : BotCommand() {
    override val command: String = "manual"
    override val permissions: List<TelegramUser.Role> = listOf(TelegramUser.Role.ADMIN)
    override val publish: Boolean = true
    override val description: String = "Обновление Readme проекта"

    override suspend fun execute(message: Message): BotApiMethod<*>? =
        SendMessage().apply {
            chatId = message.chatId.toString()
            text = when (storageService.updateManual()) {
                StorageService.Result.SUCCESS -> "Обновление readme успешно запущено"
                else -> "Ошибка выполнения команды"
            }
        }
}