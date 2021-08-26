package com.lezenford.mfr.server.controller

import com.lezenford.mfr.server.security.TelegramAuthentication
import com.lezenford.mfr.server.telegram.TelegramBot
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update
import ru.fullrest.mfr.common.extensions.Logger

@RestController
@RequestMapping("telegram")
class TelegramController(
    private val telegramBot: TelegramBot,
    private val authenticationManager: AuthenticationManager,
) {
    @RequestMapping(value = ["/\${telegram.token}"], method = [RequestMethod.POST])
    fun onUpdateReceived(@RequestBody update: Update): ResponseEntity<BotApiMethod<*>> {
        return kotlin.runCatching {
            secureFilter(update)
            telegramBot.receiveMessage(update)
        }.onFailure {
            log.error(it.message)
            log.debug(it)
        }.getOrNull()?.let { ResponseEntity.ok(it) } ?: ResponseEntity.ok().build()
    }

    private fun secureFilter(update: Update) {
        (update.message?.from ?: update.callbackQuery?.from)?.also {
            authenticationManager.authenticate(TelegramAuthentication(id = it.id, name = it.userName))
        }
    }

    companion object {
        private val log by Logger()
    }
}