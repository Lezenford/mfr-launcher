package com.lezenford.mfr.server.telegram.command

import com.lezenford.mfr.server.telegram.component.BotCommand
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class HelpCommand : BotCommand() {
    override val command: String = "help"

    override fun execute(message: Message): BotApiMethod<*>? {
        return SendMessage(message.chat.id.toString(), "Список команд:")
    }
}