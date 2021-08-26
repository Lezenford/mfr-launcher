package com.lezenford.mfr.server.event

import org.springframework.context.ApplicationEvent
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import java.io.Serializable

class SendMessageEvent(source: Any, val message: BotApiMethod<out Serializable>) : ApplicationEvent(source)