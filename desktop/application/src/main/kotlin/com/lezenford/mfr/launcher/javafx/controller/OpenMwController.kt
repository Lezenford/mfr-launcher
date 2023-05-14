package com.lezenford.mfr.launcher.javafx.controller

import com.lezenford.mfr.launcher.service.OpenMwService
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import kotlinx.coroutines.launch
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import com.lezenford.mfr.javafx.component.FxController

@Profile("GUI")
@Component
class OpenMwController(
    owner: LauncherController,
    private val openMwService: OpenMwService
) : FxController("fxml/openmw.fxml", owner) {
    private val high: ToggleButton by fxml()
    private val middle: ToggleButton by fxml()
    private val low: ToggleButton by fxml()
    private val basic: ToggleButton by fxml()
    private val custom: ToggleButton by fxml()
    private val settings: ToggleGroup = high.toggleGroup

    init {
        stage.setOnShowing { selectActiveToggle() }
        stage.addEventHandler(KeyEvent.KEY_PRESSED) {
            it.takeIf { it.code == KeyCode.ESCAPE }?.run { hide() }
        }

        settings.selectedToggleProperty().addListener { _, oldValue, newValue ->
            launch {
                when (newValue) {
                    high -> openMwService.applyConfig(OpenMwService.Configuration.HIGH, oldValue === custom)
                    middle -> openMwService.applyConfig(OpenMwService.Configuration.MIDDLE, oldValue === custom)
                    low -> openMwService.applyConfig(OpenMwService.Configuration.LOW, oldValue === custom)
                    basic -> openMwService.applyConfig(OpenMwService.Configuration.BASIC, oldValue === custom)
                    custom -> openMwService.applyConfig(OpenMwService.Configuration.CUSTOM, false)
                }
                selectActiveToggle()
            }
        }
    }

    private fun selectActiveToggle() {
        launch {
            openMwService.findActiveConfig()?.let {
                when (it) {
                    OpenMwService.Configuration.HIGH -> high
                    OpenMwService.Configuration.MIDDLE -> middle
                    OpenMwService.Configuration.LOW -> low
                    OpenMwService.Configuration.BASIC -> basic
                    OpenMwService.Configuration.CUSTOM -> custom
                }
            }.also { button ->
                settings.selectToggle(button)
                custom.isDisable = button != null && (button === custom).not()
            }
        }
    }
}