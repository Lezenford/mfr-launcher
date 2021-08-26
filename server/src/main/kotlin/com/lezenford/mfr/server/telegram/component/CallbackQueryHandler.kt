package com.lezenford.mfr.server.telegram.component

import com.lezenford.mfr.server.telegram.component.element.CallbackData
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import ru.fullrest.mfr.common.extensions.Logger

@Component
class CallbackQueryHandler(
    private val modules: Collection<CallbackModule>
) {
//    private val modules = modules.associateBy { it.type }.also { log.error(it) }

    fun execute(update: Update): BotApiMethod<*> {
        if (update.hasCallbackQuery()) {
            return update.callbackQuery.data?.let {
                kotlin.runCatching {
                    val data = CallbackData(update.callbackQuery.data)
                    modules.find { it.type == data.module }?.process(update.callbackQuery, data)
                }.onFailure { log.error("Callback query processing error", it) }.getOrNull()
            } ?: AnswerCallbackQuery(update.callbackQuery.id)
        } else {
            throw TelegramApiException("Callback module can execute only updates with callback query")
        }
    }

    companion object {
        private val log by Logger()
    }
}