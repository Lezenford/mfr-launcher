package com.lezenford.mfr.server.telegram.component

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Component
class CallbackMessageHandler {
    private val callbackAnswerMap: ConcurrentMap<Long, (Update) -> BotApiMethod<*>?> = ConcurrentHashMap()

    operator fun get(key: Long): ((Update) -> BotApiMethod<*>?)? = callbackAnswerMap.remove(key)
    operator fun set(key: Long, action: Update.() -> BotApiMethod<*>?) {
        callbackAnswerMap[key] = action
    }
}