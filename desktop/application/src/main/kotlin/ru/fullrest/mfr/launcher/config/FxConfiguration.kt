package ru.fullrest.mfr.launcher.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.fullrest.mfr.launcher.javafx.controller.StartController

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