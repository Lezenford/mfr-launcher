package com.lezenford.mfr.server.telegram.callback_module

import com.lezenford.mfr.server.service.model.StatisticService
import com.lezenford.mfr.server.telegram.component.CallbackModule
import com.lezenford.mfr.server.telegram.component.element.Keyboard
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Chat
import ru.fullrest.mfr.common.extensions.Logger
import java.util.*

@Component
@PreAuthorize("hasAuthority('ADMIN')")
class StatisticsCallbackModule(
    private val statisticService: StatisticService
) : CallbackModule() {
    override val type: Type = Type.INFO

    override fun init(): Chat.() -> BotApiMethod<*>? = {
        sendMessage(
            text = statisticService.statistic(),
            keyboard = Keyboard {
                line {
                    text = "Обновить"
                    callbackData = callbackData(Event.REFRESH)
                }
                line {
                    text = "Закрыть"
                    callbackData = callbackData(Event.CLOSE)
                }
            }
        )
    }

    override fun process(): Process.() -> BotApiMethod<*>? = {
        when (Event.values()[data.event]) {
            Event.REFRESH -> editMessage(
                text = statisticService.statistic(),
                keyboard = Keyboard {
                    line {
                        text = "Обновить"
                        callbackData = callbackData(Event.REFRESH, mapOf(TOKEN to Date().time.toString()))
                    }
                    line {
                        text = "Закрыть"
                        callbackData = callbackData(Event.CLOSE)
                    }
                }
            )
            Event.CLOSE -> editMessage(text = "Для доступа к статистике воспользуйтесь командой /statistics")
        }
    }

    enum class Event {
        REFRESH, CLOSE
    }

    companion object {
        private val log by Logger()
        private const val TOKEN = "token"
    }
}