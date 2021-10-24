package com.lezenford.mfr.server.telegram

import com.lezenford.mfr.server.configuration.properties.TelegramProperties
import com.lezenford.mfr.server.event.SendMessageEvent
import com.lezenford.mfr.server.telegram.component.CallbackMessageHandler
import com.lezenford.mfr.server.telegram.component.CallbackQueryHandler
import com.lezenford.mfr.server.telegram.component.CommandHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import ru.fullrest.mfr.common.extensions.Logger
import java.io.Serializable
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

@Component
class TelegramBot(
    private val telegramProperties: TelegramProperties,
    private val commandRegistry: CommandHandler,
    private val callbackQueryHandler: CallbackQueryHandler,
    private val callbackMessageHandler: CallbackMessageHandler,
    private val restTemplate: RestTemplate
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    private val url = "https://api.telegram.org/bot${telegramProperties.token}/"

    @EventListener(ApplicationStartedEvent::class)
    fun initWebHook() {
        sendMessage(SetWebhook(telegramProperties.path + telegramProperties.token))
            ?: kotlin.run {
                log.error("Webhook is not installed!")
                exitProcess(0)
            }
        sendMessage(SetMyCommands().apply {
            commands = commandRegistry.publicCommands().map {
                BotCommand(it.command, it.description)
            }
        }) ?: log.error("Error set bot commands")
    }

    fun <T : Serializable> sendMessage(message: BotApiMethod<T>): T? {
        val httpEntity = HttpEntity<BotApiMethod<T>>(
            message,
            HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON }
        )
        return kotlin.runCatching {
            val responseEntity =
                restTemplate.exchange<String>("$url${message.method}", HttpMethod.POST, httpEntity)
            return if (responseEntity.statusCode == HttpStatus.OK) {
                message.deserializeResponse(responseEntity.body)
            } else {
                log.error("Incorrect telegram api invoke: $responseEntity")
                null
            }
        }.onFailure { log.error(it) }.getOrNull()
    }

    fun receiveMessage(update: Update): BotApiMethod<*>? {
        if (update.hasMessage() && update.message.isCommand) {
            callbackMessageHandler[update.message.chatId]
            return commandRegistry.execute(update)
        }
        if (update.hasCallbackQuery()) {
            return callbackQueryHandler.execute(update)
        }
        if (update.hasMessage()) {
            return callbackMessageHandler[update.message.chatId]?.invoke(update)
        }
        return null
    }

    @EventListener(SendMessageEvent::class)
    fun onApplicationEvent(event: SendMessageEvent) {
        sendMessage(event.message)
    }

    companion object {
        private val log by Logger()
    }
}