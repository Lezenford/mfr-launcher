package com.lezenford.mfr.manager.telegram

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.manager.configuration.properties.TelegramProperties
import com.lezenford.mfr.manager.telegram.component.CommandHandler
import com.lezenford.mfr.manager.telegram.component.MenuHandler
import com.lezenford.mfr.manager.telegram.component.ReceiveMessageHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.boot.CommandLineRunner
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand
import java.io.Serializable
import kotlin.coroutines.CoroutineContext
import kotlin.system.exitProcess

@Component
class TelegramBot(
    private val properties: TelegramProperties,
    private val commandRegistry: CommandHandler,
    private val menuHandler: MenuHandler,
    private val receiveMessageHandler: ReceiveMessageHandler,
    private val telegramWebClient: WebClient,
    private val messageQueue: SharedFlow<BotApiMethod<out Serializable>>
) : CoroutineScope, CommandLineRunner {
    override val coroutineContext: CoroutineContext = Dispatchers.Default

    override fun run(vararg args: String) {
        launch {
            initWebHook()
            messageQueue.collect { sendMessage(it) }
        }
    }

    suspend fun initWebHook() {
        if (properties.registerWebhook) {
            sendMessage(SetWebhook(properties.path + properties.token))?.also {
                log.info("Webhook has been successfully set")
            } ?: kotlin.run {
                log.error("Webhook is not installed!")
                exitProcess(0)
            }
        }
        if (properties.registerCommands) {
            sendMessage(SetMyCommands().apply {
                commands = commandRegistry.publicCommands().map {
                    BotCommand(it.command, it.description)
                }
            })?.also {
                log.info("Commands have been successfully imported")
            } ?: log.error("Error set bot commands")
        }
    }

    suspend fun receiveMessage(update: Update): BotApiMethod<*>? = when {
        update.hasMessage() && update.message.isCommand -> {
            receiveMessageHandler[update.message.chatId]
            commandRegistry.execute(update)
        }

        update.hasCallbackQuery() -> menuHandler.handleCallbackMessage(update.callbackQuery)
        update.hasMessage() -> receiveMessageHandler[update.message.chatId]?.invoke(update)
        else -> null
    }

    private suspend fun <T : Serializable> sendMessage(message: BotApiMethod<T>): T? {
        return telegramWebClient.post()
            .uri(message.method)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(message)
            .retrieve()
            .bodyToMono<String>()
            .map { message.deserializeResponse(it) }
            .doOnError { log.error("Incorrect telegram api invoke: ${it.message}", it) }
            .awaitSingleOrNull()
    }

    companion object {
        private val log by Logger()
    }
}