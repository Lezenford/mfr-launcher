package com.lezenford.mfr.server.telegram.command

import com.lezenford.mfr.server.telegram.callback_module.StatisticsCallbackModule
import com.lezenford.mfr.server.telegram.component.BotCommand
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class StatisticsCommand(
    private val statisticsCallbackModule: StatisticsCallbackModule
) : BotCommand() {
    override val command: String = "statistics"
    override val description: String = "Статистика сервера"
    override val publish: Boolean = true

    @PreAuthorize("hasAuthority('ADMIN')")
    override fun execute(message: Message): BotApiMethod<*>? {
        return statisticsCallbackModule.init(message.chat)
    }
}