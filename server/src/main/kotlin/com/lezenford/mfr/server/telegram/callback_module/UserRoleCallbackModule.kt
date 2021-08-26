package com.lezenford.mfr.server.telegram.callback_module

import com.lezenford.mfr.server.model.entity.TelegramUser
import com.lezenford.mfr.server.service.model.TelegramUserService
import com.lezenford.mfr.server.telegram.component.CallbackModule
import com.lezenford.mfr.server.telegram.component.element.Keyboard
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Chat

@Component
@PreAuthorize("hasAuthority('ADMIN')")
class UserRoleCallbackModule(private val telegramUserService: TelegramUserService) : CallbackModule() {
    override val type: Type = Type.ROLE

    override fun init(): Chat.() -> BotApiMethod<*>? = {
        sendMessage(
            text = "Выберите пользователя, которому необходимо изменить роль",
            keyboard = mainKeyboard()
        )
    }

    override fun process(): Process.() -> BotApiMethod<*>? = {
        when (Event.values()[data.event]) {
            Event.CANCEL -> editMessage(text = "Запрос на изменение прав отменен")
            Event.BACK -> editMessage(
                text = "Выберите пользователя, которому необходимо изменить роль",
                keyboard = mainKeyboard()
            )
            Event.SELECT_USER -> {
                val userId = data.details[USER_ID_PROPERTY]?.toLong()
                    ?: throw IllegalArgumentException("Incorrect user id property")
                telegramUserService.findById(userId)?.run {
                    editMessage(
                        text = "Выберите роль для пользователя $username",
                        keyboard = Keyboard {
                            lines(TelegramUser.Role.values().toList()) {
                                text = it.name
                                callbackData = callbackData(
                                    event = Event.SELECT_ROLE,
                                    details = mapOf(USER_ID_PROPERTY to id.toString(), ROLE_PROPERTY to it.name)
                                )
                            }
                            line {
                                text = "Назад"
                                callbackData = callbackData(Event.BACK)
                            }
                        }
                    )
                }
            }
            Event.SELECT_ROLE -> {
                val userId = data.details[USER_ID_PROPERTY]?.toLong()
                    ?: throw IllegalArgumentException("Incorrect user id property")
                val role = data.details[ROLE_PROPERTY]?.let { TelegramUser.Role.valueOf(it) }
                    ?: throw IllegalArgumentException("Incorrect Role property")
                telegramUserService.findById(userId)?.run {
                    this.role = role
                    telegramUserService.save(this)
                    editMessage(text = "Пользователю $username успешно установлена роль $role")
                }
            }
        }
    }

    private fun mainKeyboard(): Keyboard =
        Keyboard {
            lines(telegramUserService.findAll()) {
                text = "${it.username}: ${it.role}"
                callbackData = callbackData(Event.SELECT_USER, mapOf(USER_ID_PROPERTY to it.id.toString()))
            }
            line {
                text = "Отмена"
                callbackData = callbackData(Event.CANCEL)
            }
        }

    private enum class Event {
        CANCEL, BACK, SELECT_USER, SELECT_ROLE
    }

    companion object {
        private const val USER_ID_PROPERTY = "1"
        private const val ROLE_PROPERTY = "2"
    }
}