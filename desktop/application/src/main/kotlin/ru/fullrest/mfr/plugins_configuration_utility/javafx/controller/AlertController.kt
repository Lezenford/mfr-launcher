package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.stage.Modality
import javafx.stage.Window
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.initController
import kotlin.system.exitProcess

fun createAlert(owner: Window? = null): AlertController = initController("fxml/alert.fxml", owner) as AlertController

class AlertController : FxController() {

    @FXML
    private lateinit var title: Label

    @FXML
    private lateinit var description: TextArea

    @FXML
    private lateinit var closeButton: Button

    override fun init() {
        stage.initModality(Modality.WINDOW_MODAL)
    }

    fun error(
        title: String = "",
        description: String
    ) {
        this.title.text = title
        this.description.text = description
        this.closeButton.onAction = EventHandler { exitProcess(0) }
        this.showAndWait()
    }

    fun info(
        title: String = "",
        description: String
    ) {
        this.title.text = title
        this.description.text = description
        this.closeButton.onAction = EventHandler { hide() }
        this.showAndWait()
    }
}