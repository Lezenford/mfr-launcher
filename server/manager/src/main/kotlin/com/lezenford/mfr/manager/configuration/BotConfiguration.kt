package com.lezenford.mfr.manager.configuration

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import java.io.Serializable

@Configuration
class BotConfiguration {

    @Bean
    fun messageQueue(): SharedFlow<BotApiMethod<out Serializable>> = MutableSharedFlow()
}