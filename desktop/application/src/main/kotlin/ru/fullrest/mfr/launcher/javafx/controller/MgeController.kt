package ru.fullrest.mfr.launcher.javafx.controller

import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.md5
import ru.fullrest.mfr.javafx.component.FxController
import ru.fullrest.mfr.javafx.extensions.runFx
import ru.fullrest.mfr.launcher.config.properties.GameProperties
import ru.fullrest.mfr.launcher.exception.ExternalApplicationException
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

@Component
class MgeController(
    owner: LauncherController,
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
            when (newValue) {
                high -> useTemplate(properties.classic.mge.templates.high, oldValue === custom)
                middle -> useTemplate(properties.classic.mge.templates.middle, oldValue === custom)
                low -> useTemplate(properties.classic.mge.templates.low, oldValue === custom)
                basic -> useTemplate(properties.classic.mge.templates.basic, oldValue === custom)
                custom -> applyConfig(properties.classic.mge.configBackup)
            }
            selectActiveToggle()
        }
    }

    fun startMge() {
        kotlin.runCatching {
            ProcessBuilder("\"${properties.classic.mge.application.absolute()}\"")
                .start().onExit().thenRun { runFx { selectActiveToggle() } }
        }.onFailure { throw ExternalApplicationException("Невозможно запустить Morrowind Graphics Extender", it) }
    }

    private fun selectActiveToggle() {
        properties.classic.mge.config.takeIf { it.exists() }?.md5()?.let { current ->
            listOf(
                properties.classic.mge.templates.high.md5() to high,
                properties.classic.mge.templates.middle.md5() to middle,
                properties.classic.mge.templates.low.md5() to low,
                properties.classic.mge.templates.basic.md5() to basic
            ).find { current.contentEquals(it.first) }?.second ?: custom
        }.also { button ->
            settings.selectToggle(button)
            custom.isDisable = properties.classic.mge.configBackup.exists().not() && (button === custom).not()
        }
    }

    fun useTemplate(template: Path, backup: Boolean) {
        if (backup) {
            copyBackup()
        }
        applyConfig(template)
    }

    private fun applyConfig(config: Path) {
        config.takeIf { it.exists() }?.copyTo(properties.classic.mge.config, overwrite = true)
            ?: log.error("Config $config doesn't exist")
    }

    private fun copyBackup() {
        properties.classic.mge.also {
            it.config.copyTo(it.configBackup.apply { parent?.createDirectories() }, overwrite = true)
        }
    }

    companion object {
        private val log by Logger()
    }
}