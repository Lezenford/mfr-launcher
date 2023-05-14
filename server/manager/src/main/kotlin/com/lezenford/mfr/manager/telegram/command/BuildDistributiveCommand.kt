package com.lezenford.mfr.manager.telegram.command

import com.lezenford.mfr.manager.model.entity.TelegramUser
import com.lezenford.mfr.manager.telegram.component.element.BotCommand
import com.lezenford.mfr.manager.telegram.component.MenuHandler
import com.lezenford.mfr.manager.telegram.component.element.Menu
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Message

@Component
class BuildDistributiveCommand(
    private val menuHandler: MenuHandler
) : BotCommand() {
    override val command: String = Companion.command
    override val description: String = "Настройка игры"
    override val publish: Boolean = true
    override val permissions: List<TelegramUser.Role> = listOf(TelegramUser.Role.ADMIN)

    override suspend fun execute(message: Message): BotApiMethod<*>? {
        return menuHandler.welcomeMessage(message, Menu.Type.BUILD)
    }

    companion object {
        const val command: String = "build"
    }
}