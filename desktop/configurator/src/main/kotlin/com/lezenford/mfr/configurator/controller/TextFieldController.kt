package com.lezenford.mfr.configurator.controller

import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.stage.StageStyle
import com.lezenford.mfr.javafx.component.FxController

class TextFieldController(
    text: String = "",
    okAction: (String) -> Unit
) : FxController(source = "fxml/text.fxml", stageStyle = StageStyle.DECORATED, css = null) {
    private val textField: TextField by fxml()
    private val okButton: Button by fxml()

    init {
        textField.text = text
        okButton.onAction = EventHandler {
            okAction(textField.text)
            close()
        }
    }
}