package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController

class ConfigurationEditorFieldController : FxController() {

    @FXML
    private lateinit var textField: TextField

    private lateinit var text: String

    override fun init() {
        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
            if (event.code == KeyCode.ESCAPE) {
                hide()
            }
            if (event.code == KeyCode.ENTER) {
                save()
            }
        }
    }

    fun showAndWait(text: String): String {
        this.text = text
        textField.text = text
        super.showAndWait()
        return this.text
    }

    fun save() {
        text = textField.text
        hide()
    }
}