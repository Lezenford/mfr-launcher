package com.lezenford.mfr.javafx

import javafx.application.Application
import javafx.stage.Stage
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Profile("UI")
@Component
@Order(-100)
class Application(private val init: (primaryStage: Stage) -> Unit = {}) : Application() {
    override fun start(primaryStage: Stage) {
        init(primaryStage)
    }
}
