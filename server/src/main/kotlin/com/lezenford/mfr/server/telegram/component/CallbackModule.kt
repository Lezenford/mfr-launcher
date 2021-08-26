package com.lezenford.mfr.server.telegram.component

import com.lezenford.mfr.server.telegram.component.element.CallbackData
import com.lezenford.mfr.server.telegram.component.element.Keyboard
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Chat
import ru.fullrest.mfr.common.extensions.Logger

abstract class CallbackModule {
    abstract val type: Type
    fun init(chat: Chat): BotApiMethod<*>? = chat.(init())()

    protected abstract fun init(): Chat.() -> BotApiMethod<*>?

    open fun process(
        query: CallbackQuery,
        queryData: CallbackData
    ): BotApiMethod<*>? = Process(query, queryData).(process())()

    protected abstract fun process(): Process.() -> BotApiMethod<*>?

    protected fun callbackData(event: Enum<*>, details: Map<String, String> = emptyMap()): String =
        CallbackData(type, event.ordinal, details).convert()

    enum class Type {
        ROLE, INFO, BUILD, LAUNCHER
    }

    protected class Process(
        val query: CallbackQuery,
        val data: CallbackData
    )

    protected fun Chat.sendMessage(text: String, keyboard: Keyboard? = null) =
        SendMessage().also {
            it.chatId = id.toString()
            it.text = text
            it.replyMarkup = keyboard
        }

    protected fun Process.editMessage(text: String, keyboard: Keyboard? = null) =
        EditMessageText().also {
            it.chatId = query.message.chatId.toString()
            it.messageId = query.message.messageId
            it.text = text
            it.replyMarkup = keyboard
        }

    companion object {
        private val log by Logger()
    }
}