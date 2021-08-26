package com.lezenford.mfr.server.telegram.command

import com.lezenford.mfr.server.telegram.callback_module.UserRoleCallbackModule
import com.lezenford.mfr.server.telegram.component.BotCommand
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class UsersCommand(
    private val userRoleCallbackModule: UserRoleCallbackModule
) : BotCommand() {
    override val command: String = "users"
    override val description: String = "Установка прав доступа для пользователей"
    override val publish: Boolean = true

    @PreAuthorize("hasAuthority('ADMIN')")
    override fun execute(message: Message): BotApiMethod<*>? {
        return userRoleCallbackModule.init(message.chat)
    }
}