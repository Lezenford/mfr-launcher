package com.lezenford.mfr.launcher.javafx.controller

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import com.lezenford.mfr.javafx.component.FxController

@Profile("GUI")
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