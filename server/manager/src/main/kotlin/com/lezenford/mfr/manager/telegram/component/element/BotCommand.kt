package com.lezenford.mfr.manager.telegram.component.element

import com.fasterxml.jackson.annotation.JsonIgnore
import com.lezenford.mfr.manager.model.entity.TelegramUser
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message

abstract class BotCommand {
    @JsonIgnore
    open val publish: Boolean = false

    abstract val command: String

    abstract val permissions: List<TelegramUser.Role>

    open val description: String = ""

    abstract suspend fun execute(message: Message): BotApiMethod<*>?

    companion object {
        const val COMMAND_INIT_CHARACTER = "/"
    }
}