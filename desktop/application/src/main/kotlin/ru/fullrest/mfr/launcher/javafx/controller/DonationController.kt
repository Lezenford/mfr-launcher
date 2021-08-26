package ru.fullrest.mfr.launcher.javafx.controller

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.springframework.stereotype.Component
import ru.fullrest.mfr.javafx.component.FxController

@Component
class DonationController(owner: LauncherController) : FxController("fxml/donation.fxml", owner) {
    init {
        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
            if (event.code == KeyCode.ESCAPE || event.code == KeyCode.ENTER) {
                hide()
            }
        }
    }
}