package com.lezenford.mfr.manager.controller

import com.lezenford.mfr.manager.telegram.TelegramBot
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update
import com.lezenford.mfr.common.extensions.Logger

@RestController
@RequestMapping("telegram")
class TelegramController(
    private val telegramBot: TelegramBot
) {
    @PostMapping(value = ["\${telegram.token}"])
    suspend fun onUpdateReceived(@RequestBody update: Update): ResponseEntity<BotApiMethod<*>> {
        return kotlin.runCatching {
            telegramBot.receiveMessage(update)
        }.onFailure {
            log.error(it.message, it)
        }.getOrNull()?.let {
            ResponseEntity.ok(it)
        } ?: ResponseEntity.ok().build()
    }

    companion object {
        private val log by Logger()
    }
}