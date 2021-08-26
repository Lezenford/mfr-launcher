package com.lezenford.mfr.server.extensions

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

fun Update.editMessageText(
    chatId: Long,
    messageId: Int,
    text: String,
    keyboard: InlineKeyboardMarkup? = null
): EditMessageText = EditMessageText().also {
    it.chatId = chatId.toString()
    it.messageId = messageId
    it.replyMarkup = keyboard
    it.text = text
}

fun Update.sendMessage(
    text: String,
    keyboard: InlineKeyboardMarkup? = null
): SendMessage = SendMessage().also {
    it.chatId = this.message.chatId.toString()
    it.text = text
    it.replyMarkup = keyboard
}