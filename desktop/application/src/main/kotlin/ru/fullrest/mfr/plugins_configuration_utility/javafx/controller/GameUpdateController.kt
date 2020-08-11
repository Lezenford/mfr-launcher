package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.event.EventHandler
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent

class GameUpdateController : AbstractProgressController() {

    override fun init() {
        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
            if (closeButton.isVisible) {
                if (event.code == KeyCode.ESCAPE || event.code == KeyCode.ENTER) {
                    hide()
                }
            }
        }
        stage.onCloseRequest = EventHandler { event -> event.consume() }

        stage.onShowing = EventHandler {
            updateProgress(0, 0)
            setDescription("")
        }
    }

    override val progressBarMinWidth: Double = PROGRESS_BAR_MIN_WIDTH
    override val progressBarMaxWidth: Double = PROGRESS_BAR_MAX_WIDTH

    companion object {
        private const val PROGRESS_BAR_MIN_WIDTH = 20.0
        private const val PROGRESS_BAR_MAX_WIDTH = 250.0
    }
}