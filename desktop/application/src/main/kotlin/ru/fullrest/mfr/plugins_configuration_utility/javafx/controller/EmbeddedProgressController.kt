package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Window
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.initController

fun createProgressWindow(owner: Window? = null): EmbeddedProgressController =
    (initController("fxml/game-update.fxml", owner) as EmbeddedProgressController).also { it.init() }

class EmbeddedProgressController : ProgressBar, FxController() {

    @FXML
    override lateinit var progressBar: HBox

    @FXML
    override lateinit var progressBarDecoration: VBox

    @FXML
    override lateinit var description: Label

    @FXML
    override lateinit var progress: Label

    @FXML
    override lateinit var closeButton: VBox

    override val progressBarMinWidth: Double = PROGRESS_BAR_MIN_WIDTH
    override val progressBarMaxWidth: Double = PROGRESS_BAR_MAX_WIDTH

    public override fun init() {
        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
            if (closeButton.isVisible) {
                if (event.code == KeyCode.ESCAPE || event.code == KeyCode.ENTER) {
                    hide()
                }
            }
        }
        stage.onCloseRequest = EventHandler { event -> event.consume() }

        stage.onShowing = EventHandler {
            closeButton.isVisible = false
            updateProgress(0, 0)
            setDescription("")
        }
    }

    companion object {
        private const val PROGRESS_BAR_MIN_WIDTH = 20.0
        private const val PROGRESS_BAR_MAX_WIDTH = 250.0
    }
}