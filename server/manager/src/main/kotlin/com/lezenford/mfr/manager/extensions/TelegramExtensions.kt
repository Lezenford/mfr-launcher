package com.lezenford.mfr.manager.extensions

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup

fun Update.sendMessage(
    text: String
): SendMessage = SendMessage().also {
    it.chatId = this.message.chatId.toString()
    it.text = text
}

fun CallbackQuery.convert(): IntArray {
    val byteArray = data?.toByteArray(Charsets.ISO_8859_1)
        ?: throw IllegalArgumentException("Callback query doesn't contain data")

    return IntArray(byteArray.size / 4).also {
        it.onEachIndexed { index, _ ->
            it[index] = read4BytesFromBuffer(byteArray, index * 4)
        }
    }
}

fun IntArray.convert(): String {
    val byteArray = ByteArray(size * 4)

    onEachIndexed { index, i ->
        write4BytesToBuffer(byteArray, index * 4, i)
    }

    return String(byteArray, Charsets.ISO_8859_1)
}

private fun read4BytesFromBuffer(buffer: ByteArray, offset: Int): Int {
    return (buffer[offset + 3].toInt() shl 24) or
        (buffer[offset + 2].toInt() and 0xff shl 16) or
        (buffer[offset + 1].toInt() and 0xff shl 8) or
        (buffer[offset + 0].toInt() and 0xff)
}

private fun write4BytesToBuffer(buffer: ByteArray, offset: Int, data: Int) {
    buffer[offset + 0] = (data shr 0).toByte()
    buffer[offset + 1] = (data shr 8).toByte()
    buffer[offset + 2] = (data shr 16).toByte()
    buffer[offset + 3] = (data shr 24).toByte()
}