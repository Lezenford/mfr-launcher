package com.lezenford.mfr.server.telegram.command

import com.lezenford.mfr.server.telegram.callback_module.OverviewCallbackModule
import com.lezenford.mfr.server.telegram.component.BotCommand
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class OverviewCommand(
    private val overviewCallbackModule: OverviewCallbackModule
) : BotCommand() {
    override val command: String = "overview"
    override val description: String = "Статистика сервера"
    override val publish: Boolean = true

    @PreAuthorize("hasAuthority('ADMIN')")
    override fun execute(message: Message): BotApiMethod<*>? {
        return overviewCallbackModule.init(message.chat)
    }
}