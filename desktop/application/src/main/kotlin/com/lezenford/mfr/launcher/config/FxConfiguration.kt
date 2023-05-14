package com.lezenford.mfr.launcher.config

import com.lezenford.mfr.launcher.javafx.controller.StartController
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FxConfiguration {

    /**
     * Объект создается до инициализации контекста
     */
    @Bean
    fun startController(): StartController {
        return startController
    }

    companion object {
        val startController: StartController by lazy { StartController() }
    }
}