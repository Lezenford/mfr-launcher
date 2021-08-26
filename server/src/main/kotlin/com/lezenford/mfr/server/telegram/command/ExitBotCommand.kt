package com.lezenford.mfr.server.telegram.command

import com.lezenford.mfr.server.service.model.TelegramUserService
import com.lezenford.mfr.server.telegram.component.BotCommand
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class ExitBotCommand(val telegramUserService: TelegramUserService) : BotCommand() {
    override val command: String = "exit"

    @PreAuthorize("hasAnyAuthority('USER','ADMIN')")
    override fun execute(message: Message): BotApiMethod<*>? {
        telegramUserService.deleteById(message.from.id)
        return SendMessage(message.chat.id.toString(), "Your account successfully deleted from bot")
    }
}