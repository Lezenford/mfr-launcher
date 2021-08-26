package ru.fullrest.mfr.configurator

import javafx.application.Application
import javafx.stage.Stage
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component
import ru.fullrest.mfr.configurator.controller.MainController

@SpringBootApplication
class Configurator : Application() {
    override fun start(primaryStage: Stage?) {
        runApplication<Configurator>()
    }
}

fun main(args: Array<String>) {
    Application.launch(Configurator::class.java, *args)
}

@Component
class Runner(
    private val mainController: MainController
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        mainController.show()
        mainController.selectGame()
    }
}