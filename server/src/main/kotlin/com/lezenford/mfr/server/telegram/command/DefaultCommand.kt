package com.lezenford.mfr.server.telegram.command

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class DefaultCommand : (Message) -> BotApiMethod<*> {
    override fun invoke(message: Message): BotApiMethod<*> =
        SendMessage(message.chat.id.toString(), "Unknown command. Please use /help")
}