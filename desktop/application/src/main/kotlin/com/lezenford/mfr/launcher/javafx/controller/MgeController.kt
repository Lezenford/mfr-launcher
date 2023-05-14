package com.lezenford.mfr.launcher.javafx.controller

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.javafx.component.FxController
import com.lezenford.mfr.javafx.extensions.runFx
import com.lezenford.mfr.launcher.config.properties.GameProperties
import com.lezenford.mfr.launcher.service.MgeService
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.io.path.exists

@Profile("GUI")
@Component
class MgeController(
    owner: LauncherController,
    private val mgeService: MgeService,
    private val properties: GameProperties
) : FxController("fxml/mge.fxml", owner) {
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
                    high -> mgeService.applyConfig(MgeService.Configuration.HIGH, oldValue === custom)
                    middle -> mgeService.applyConfig(MgeService.Configuration.MIDDLE, oldValue === custom)
                    low -> mgeService.applyConfig(MgeService.Configuration.LOW, oldValue === custom)
                    basic -> mgeService.applyConfig(MgeService.Configuration.BASIC, oldValue === custom)
                    custom -> mgeService.applyConfig(MgeService.Configuration.CUSTOM, false)
                }
                selectActiveToggle()
            }
        }
    }

    fun startMge() {
        launch(Dispatchers.IO) {
            mgeService.startMge().onExit().whenComplete { _, _ -> runFx { selectActiveToggle() } }
        }
    }

    private fun selectActiveToggle() {
        launch {
            mgeService.findActiveConfig()?.let {
                when (it) {
                    MgeService.Configuration.HIGH -> high
                    MgeService.Configuration.MIDDLE -> middle
                    MgeService.Configuration.LOW -> low
                    MgeService.Configuration.BASIC -> basic
                    MgeService.Configuration.CUSTOM -> custom
                }
            }.also { button ->
                settings.selectToggle(button)
                custom.isDisable = properties.classic.mge.configBackup.exists().not() && (button === custom).not()
            }
        }
    }

    companion object {
        private val log by Logger()
    }
}