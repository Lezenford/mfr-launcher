package com.lezenford.mfr.manager.telegram.component

import com.lezenford.mfr.manager.extensions.convert
import com.lezenford.mfr.manager.service.TelegramUserService
import com.lezenford.mfr.manager.telegram.component.element.Button
import com.lezenford.mfr.manager.telegram.component.element.Menu
import com.lezenford.mfr.manager.telegram.component.element.MenuItem
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

@Component
class MenuHandler(
    callbackModules: List<Menu>,
    private val telegramUserService: TelegramUserService
) {
    private val callbackModules: Map<Menu.Type, Menu> = callbackModules.associateBy { it.identity }

    suspend fun handleCallbackMessage(callbackQuery: CallbackQuery): BotApiMethod<*> {
        return telegramUserService.findById(callbackQuery.from.id)?.let start@{ user ->
            val identity = callbackQuery.convert()
            Menu.Type.values().find { it.ordinal == identity.first() }?.let { type ->
                callbackModules[type]?.takeIf { it.permissions.contains(user.role) }?.let { module ->
                    var button: Button = module.firstMenuItem.await().findButton(identity[1])
                        ?: return@start null

                    for (i in 2 until identity.size) {
                        button = button.menuItem.findButton(identity[i])
                            ?: return@start null
                    }

                    button.also { it.action(callbackQuery) }.menuItem.let { message ->
                        EditMessageText().apply {
                            messageId = callbackQuery.message.messageId
                            chatId = callbackQuery.message.chatId.toString()
                            text = message.text.await()
                            replyMarkup = message.toKeyboard()
                        }
                    }
                }
            }
        } ?: AnswerCallbackQuery(callbackQuery.id)
    }

    suspend fun welcomeMessage(message: Message, type: Menu.Type): BotApiMethod<*>? {
        return callbackModules[type]?.let { callbackModule ->
            val context = callbackModule.firstMenuItem.await()
            SendMessage().apply {
                chatId = message.from.id.toString()
                text = context.text.await()
                replyMarkup = context.toKeyboard()
            }
        }
    }

    private fun MenuItem.findButton(value: Int): Button? =
        buttons.find { it.menuItem.identity.last() == value }

    private fun MenuItem.toKeyboard(): InlineKeyboardMarkup {
        return InlineKeyboardMarkup().apply {
            keyboard = buttons.map {
                listOf(InlineKeyboardButton().apply {
                    callbackData = it.menuItem.identity.convert()
                    text = it.text
                })
            }
        }
    }
}