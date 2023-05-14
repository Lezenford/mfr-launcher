package com.lezenford.mfr.manager.telegram.component

import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Component
class ReceiveMessageHandler {
    private val callbackAnswerMap: ConcurrentMap<Long, suspend (Update) -> BotApiMethod<*>?> = ConcurrentHashMap()

    operator fun get(key: Long): (suspend (Update) -> BotApiMethod<*>?)? = callbackAnswerMap.remove(key)
    operator fun set(key: Long, action: suspend Update.() -> BotApiMethod<*>?) {
        callbackAnswerMap[key] = action
    }
}