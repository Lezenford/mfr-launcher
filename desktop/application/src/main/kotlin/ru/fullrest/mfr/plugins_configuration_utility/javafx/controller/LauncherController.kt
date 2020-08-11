package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.util.Duration
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import ru.fullrest.mfr.plugins_configuration_utility.PluginsConfigurationUtilityApplication
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import java.io.IOException
import kotlin.system.exitProcess

class LauncherController : FxController() {

    @Autowired
    private lateinit var application: PluginsConfigurationUtilityApplication

    @Autowired
    private lateinit var alertController: AlertController

    @Autowired
    private lateinit var helpForProjectController: HelpForProjectController

    @Autowired
    private lateinit var pluginConfigurationController: PluginConfigurationController

    @Autowired
    private lateinit var readmeController: ReadmeController

    @Autowired
    private lateinit var mgeConfigurationController: MgeConfigurationController

    @Autowired
    private lateinit var welcomeController: WelcomeController

    @Autowired
    private lateinit var openMwConfigurationController: OpenMwConfigurationController

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    @Autowired
    private lateinit var files: ApplicationFiles

    @FXML
    private lateinit var betaLabel: VBox

    @FXML
    private lateinit var gameVersion: Label

    @FXML
    private lateinit var applicationVersion: Label

    @FXML
    private lateinit var gamePath: Label

    @FXML
    private lateinit var playMge: Button

    @FXML
    private lateinit var launcher: Button

    @FXML
    private lateinit var mcp: Button

    @FXML
    private lateinit var mge: Button

    @FXML
    private lateinit var openMw: Button

    @FXML
    private lateinit var openMwLauncher: Button

    override fun init() {
        stage.onShowing = EventHandler {
            gameVersion.text = applicationProperties.gameVersion
            applicationVersion.text = applicationProperties.applicationVersion
            gamePath.text = files.gameFolder.absolutePath
            gamePath.tooltip = Tooltip(gamePath.text)
            files.morrowind.also {
                if (it.exists().not()) {
                    setTooltipAndDisableButton(playMge, "Файл Morrowind.exe не найден")
                }
            }
            files.launcher.also {
                if (it.exists().not()) {
                    setTooltipAndDisableButton(launcher, "Файл Morrowind Launcher.exe не найден")
                }
            }
            files.mcp.also {
                if (it.exists().not()) {
                    setTooltipAndDisableButton(mcp, "Файл Morrowind Code Patch.exe не найден")
                }
            }
            files.mge.also {
                if (it.exists().not()) {
                    setTooltipAndDisableButton(mge, "Файл MGEXEgui.exe не найден")
                }
            }
            files.openMw.also {
                if (it.exists().not()) {
                    setTooltipAndDisableButton(openMw, "Файл openmw.exe не найден")
                }
            }
            files.openMwLauncher.also {
                if (it.exists().not()) {
                    setTooltipAndDisableButton(openMwLauncher, "Файл openmw-launcher.exe не найден")
                }
            }
        }

        mgeConfigurationController.setOwnerAndModality(stage, Modality.APPLICATION_MODAL)
        readmeController.setOwnerAndModality(stage, Modality.APPLICATION_MODAL)
        helpForProjectController.setOwnerAndModality(stage, Modality.APPLICATION_MODAL)
        welcomeController.setOwnerAndModality(stage, Modality.APPLICATION_MODAL)
        openMwConfigurationController.setOwnerAndModality(stage, Modality.APPLICATION_MODAL)

        betaLabel.isVisible = applicationProperties.beta
    }

    fun startGame() {
        try {
            Runtime.getRuntime().exec("\"${files.morrowind}\"", null, files.gameFolder)
            exitProcess(0)
        } catch (e: IOException) {
            createAlertForException(e, "Невозможно запустить Morrowind!")
        }
    }

    fun startOpenMw() {
        try {
            Runtime.getRuntime().exec("\"${files.openMw}\"", null, files.gameFolder)
            Platform.exit()
        } catch (e: IOException) {
            createAlertForException(e, "Невозможно запустить Open Morrowind!")
        }
    }

    fun startOpenMwLauncher() {
        try {
            Runtime.getRuntime().exec("\"${files.openMwLauncher}\"", null, files.gameFolder)
            Platform.exit()
        } catch (e: IOException) {
            createAlertForException(e, "Невозможно запустить Open Morrowind Launcher!")
        }
    }

    fun startLauncher() {
        try {
            Runtime.getRuntime().exec("\"${files.launcher}\"", null, files.gameFolder)
            Platform.exit()
        } catch (e: IOException) {
            createAlertForException(e, "Невозможно запустить Morrowind Launcher!")
        }
    }

    fun startMCP() {
        try {
            Runtime.getRuntime().exec("\"${files.mcp}\"", null, files.gameFolder)
        } catch (e: IOException) {
            var exception = e
            if (exception.message!!.contains("error=740")) {
                exception = IOException("Перезапустите конфигуратор от имени администратора")
            }
            createAlertForException(exception, "Невозможно запустить MCP!")
        }
    }

    fun startMGE() {
        mgeConfigurationController.show()
    }

    fun startOpenMwConfig() {
        openMwConfigurationController.show()
    }

    fun openForum() {
        application.hostServices.showDocument(applicationProperties.forumLink)
    }

    fun openReadme() {
        hide()
        readmeController.show()
    }

    fun helpForProject() {
        helpForProjectController.show()
    }

    fun openDiscord(){
        application.hostServices.showDocument(applicationProperties.discordLink)
    }

    fun checkUpdate() {
    }

    fun openConfiguration() {
        hide()
        pluginConfigurationController.show()
    }

    private fun createAlertForException(e: Throwable, message: String) {
        launch {
            alertController.error(
                title = message,
                exception = e,
                closeButtonEvent = EventHandler { alertController.hide() }
            )
        }
    }

    private fun setTooltipAndDisableButton(button: Button, text: String) {
        button.tooltip = Tooltip(text).also {
            it.showDelay = Duration.seconds(0.3)
        }
        button.onAction = EventHandler { obj: ActionEvent -> obj.consume() }
        button.opacity = 0.7
    }

    fun close() {
        Platform.exit()
    }
}