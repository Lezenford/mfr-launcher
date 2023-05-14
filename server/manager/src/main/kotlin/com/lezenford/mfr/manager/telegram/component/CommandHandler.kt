package com.lezenford.mfr.manager.telegram.component

import com.lezenford.mfr.manager.service.TelegramUserService
import com.lezenford.mfr.manager.telegram.component.element.BotCommand
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class CommandHandler(
    commands: Collection<BotCommand>,
    private val telegramUserService: TelegramUserService
) {
    private val commands: Map<String, BotCommand> = commands.associateBy { it.command }

    suspend fun execute(update: Update): BotApiMethod<*>? {
        if (update.message?.isCommand == true) {
            val command = update.message.text.takeWhile { it != ' ' }.drop(1).lowercase()
            val role = telegramUserService.findById(update.message.from.id)?.role
            return commands[command]?.takeIf { it.permissions.isEmpty() || it.permissions.contains(role) }
                ?.execute(update.message)
                ?: SendMessage().apply {
                    chatId = update.message.chatId.toString()
                    text = "Unknown command. Please use /help"
                }
        } else {
            throw IllegalArgumentException("Update $update doesn't contain command")
        }
    }

    fun publicCommands(): List<BotCommand> = commands.values.filter { it.publish }
}