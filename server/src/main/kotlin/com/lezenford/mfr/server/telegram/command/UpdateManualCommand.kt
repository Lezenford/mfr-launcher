package com.lezenford.mfr.server.telegram.command

import com.lezenford.mfr.server.service.UpdaterProcessorExecutor
import com.lezenford.mfr.server.telegram.component.BotCommand
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class UpdateManualCommand(
    private val updaterProcessorExecutor: UpdaterProcessorExecutor
) : BotCommand() {
    override val command: String = "manual"
    override val publish: Boolean = true
    override val description: String = "Обновление Readme проекта"

    @PreAuthorize("hasAuthority('ADMIN')")
    override fun execute(message: Message): BotApiMethod<*>? =
        updaterProcessorExecutor.updateManual().let {
            if (it) {
                SendMessage(message.chatId.toString(), "Запущено обновление Readme")
            } else {
                SendMessage(message.chatId.toString(), "Другая задача на обновление уже запущена")
            }
        }
}