package com.lezenford.mfr.server.telegram.command

import com.lezenford.mfr.server.telegram.callback_module.LauncherCallbackModule
import com.lezenford.mfr.server.telegram.component.BotCommand
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class LauncherDistributiveCommand(
    private val launcherCallbackModule: LauncherCallbackModule
) : BotCommand() {
    override val command: String = "launcher"
    override val description: String = "Настройка лаунчера"
    override val publish: Boolean = true

    @PreAuthorize("hasAuthority('ADMIN')")
    override fun execute(message: Message): BotApiMethod<*>? = launcherCallbackModule.init(message.chat)

}