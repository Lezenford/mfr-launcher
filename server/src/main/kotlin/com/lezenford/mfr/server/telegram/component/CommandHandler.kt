package com.lezenford.mfr.server.telegram.component

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import ru.fullrest.mfr.common.extensions.Logger

@Component
class CommandHandler(
    commands: Collection<BotCommand>,
    private val defaultCommand: (Message) -> BotApiMethod<*>
) {
    private val commands: Map<String, BotCommand> = commands.associateBy { it.command }

    fun execute(update: Update): BotApiMethod<*>? {
        if (update.message?.isCommand == true) {
            val command = update.message.text.takeWhile { it != ' ' }.drop(1).lowercase()
            return commands[command]?.execute(update.message) ?: defaultCommand(update.message)
        } else {
            throw IllegalArgumentException("Update $update doesn't contain command")
        }
    }

    fun publicCommands(): List<BotCommand> = commands.values.filter { it.publish }

    companion object {
        private val log by Logger()
    }
}