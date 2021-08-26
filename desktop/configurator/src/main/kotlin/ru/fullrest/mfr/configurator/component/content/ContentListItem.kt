package ru.fullrest.mfr.configurator.component.content

import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ListView

class ContentListItem(
    name: String,
    parent: ListView<ContentListItem>,
    removable: Boolean = true
) : ContentItem(name) {
    init {
        if (removable) {
            Button("-").also { button ->
                children.add(button)
                button.setOnMouseClicked {
                    Alert(
                        Alert.AlertType.CONFIRMATION,
                        "Удалить $name?",
                        ButtonType.CANCEL,
                        ButtonType.OK
                    ).showAndWait().filter { it.buttonData.isCancelButton.not() }.ifPresent {
                        parent.items.remove(this)
                    }
                }
            }
        }
    }
}