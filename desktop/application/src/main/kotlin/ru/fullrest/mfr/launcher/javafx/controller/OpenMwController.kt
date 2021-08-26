package ru.fullrest.mfr.launcher.javafx.controller

import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.md5
import ru.fullrest.mfr.javafx.component.FxController
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.config.properties.GameProperties
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

@Component
class OpenMwController(
    owner: LauncherController,
    private val applicationProperties: ApplicationProperties,
    private val gameProperties: GameProperties
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
            when (newValue) {
                high -> useTemplate(gameProperties.openMw.templates.high, oldValue === custom)
                middle -> useTemplate(gameProperties.openMw.templates.middle, oldValue === custom)
                low -> useTemplate(gameProperties.openMw.templates.low, oldValue === custom)
                basic -> useTemplate(gameProperties.openMw.templates.basic, oldValue === custom)
                custom -> applyConfig(gameProperties.openMw.configBackupFolder)
            }
            selectActiveToggle()
        }
    }

    private fun selectActiveToggle() {
        gameProperties.openMw.configFolder
            //Сравнивать только если существует текущий конфиг
            .takeIf { configFolder -> configFolder.configFiles().all { it.exists() } }
            ?.let { current ->
                listOf(
                    gameProperties.openMw.templates.high to high,
                    gameProperties.openMw.templates.middle to middle,
                    gameProperties.openMw.templates.low to low,
                    gameProperties.openMw.templates.basic to basic
                ).find { current.equalsConfig(it.first) }?.second ?: custom
            }.also {
                settings.selectToggle(it)
            }
    }

    fun useTemplate(template: Path, backup: Boolean) {
        if (backup) {
            copyBackup()
        }
        applyConfig(template)
    }

    //TODO подумать о разделении функций контроллера и сервиса
    fun prepareTemplates() {
        val generator: (Path) -> Unit = { folder ->
            folder.resolve(templateConfigFileName).takeIf { it.exists() }?.also { template ->
                template.readLines().map {
                    it.replace(
                        gameProperties.openMw.configChangeValue,
                        applicationProperties.gameFolder.absolutePathString()
                    )
                }.also {
                    folder.resolve(configFileName).writeLines(it)
                }
            }
        }

        generator(gameProperties.openMw.templates.basic)
        generator(gameProperties.openMw.templates.low)
        generator(gameProperties.openMw.templates.middle)
        generator(gameProperties.openMw.templates.high)
    }

    private fun applyConfig(config: Path?) {
        //Применять только если у конфига есть все требуемые файлы
        config?.takeIf { configFolder -> configFolder.configFiles().all { it.exists() } }
            //Удалить текущий конфиг
            ?.also { gameProperties.openMw.configFolder.configFiles().forEach { it.deleteIfExists() } }
            ?.configFiles()?.forEach {
                it.copyTo(gameProperties.openMw.configFolder.resolve(it.fileName), overwrite = true)
            } ?: log.error("Config $config doesn't exist")
    }

    private fun copyBackup() {
        gameProperties.openMw.takeIf { openMw ->
            listOf(
                openMw.templates.high,
                openMw.templates.middle,
                openMw.templates.low,
                openMw.templates.basic
            ).none { it.equalsConfig(openMw.configFolder) }
        }?.also { openMw ->
            openMw.configFolder.configFiles()
                .takeIf { files -> files.all { it.exists() } }?.forEach {
                    it.copyTo(
                        openMw.configBackupFolder.resolve(it.fileName).apply { parent?.createDirectories() },
                        overwrite = true
                    )
                }
        }
    }

    private fun Path.configFiles() =
        configFiles.map {
            this.resolve(it).absolute()
        }

    private fun Path.equalsConfig(target: Path?): Boolean {
        return this.configFiles().let { source ->
            source.all {
                it.md5().contentEquals(target?.resolve(it.fileName)?.md5())
            }
        }
    }

    companion object {
        private val log by Logger()
        private const val configFileName = "openmw.cfg"
        private val configFiles = listOf("input_v3.xml", "launcher.cfg", configFileName, "settings.cfg")
        private const val templateConfigFileName = "openmw_template.cfg"
    }
}