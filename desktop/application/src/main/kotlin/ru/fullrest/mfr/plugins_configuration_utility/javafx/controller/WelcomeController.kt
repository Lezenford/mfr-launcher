package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController

class WelcomeController : FxController() {
    override fun init() {
        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
            if (event.code == KeyCode.ESCAPE || event.code == KeyCode.ENTER) {
                hide()
            }
        }
    }
}