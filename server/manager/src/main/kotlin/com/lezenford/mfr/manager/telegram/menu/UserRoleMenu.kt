package com.lezenford.mfr.manager.telegram.menu

import com.lezenford.mfr.manager.configuration.MODULE_CACHE
import com.lezenford.mfr.manager.model.entity.TelegramUser
import com.lezenford.mfr.manager.service.TelegramUserService
import com.lezenford.mfr.manager.telegram.command.UsersCommand
import com.lezenford.mfr.manager.telegram.component.element.Menu
import com.lezenford.mfr.manager.telegram.component.element.MenuBuilder
import com.lezenford.mfr.manager.telegram.component.element.MenuItem
import com.lezenford.mfr.manager.telegram.component.element.menu
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.asFlow
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class UserRoleMenu(
    private val telegramUserService: TelegramUserService,
) : Menu() {
    override val identity: Type = Type.USER_ROLE
    override val permissions: List<TelegramUser.Role> = listOf(TelegramUser.Role.ADMIN)

    @get:Cacheable(value = [MODULE_CACHE], key = "'UserRoleCallbackModule'")
    override val firstMenuItem: Deferred<MenuItem>
        get() = menu("Для изменения прав воспользуйтесь командой /${UsersCommand.command}") {
            text = "Выберите пользователя, которому необходимо изменить роль"
            buttons(telegramUserService.findAll()) { telegramUser ->
                id = telegramUser.telegramId.toInt()
                text = telegramUser.username
                message = {
                    text = "Выберите роль для пользователя ${telegramUser.username}"
                    buttons(TelegramUser.Role.values().asFlow()) { role ->
                        text = role.name
                        message = {
                            text = "Пользователю ${telegramUser.username} успешно установлена роль $role"

                        }
                        action = {
                            telegramUser.role = role
                            telegramUserService.save(telegramUser)
                        }
                    }
                    button(MenuBuilder.Action.BACK)
                    button(MenuBuilder.Action.CLOSE)
                }
            }
            button(MenuBuilder.Action.CLOSE)
        }
}