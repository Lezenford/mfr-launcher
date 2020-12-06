package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.animation.RotateTransition
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.VBox
import javafx.util.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Autowired
import ru.fullrest.mfr.api.Links
import ru.fullrest.mfr.plugins_configuration_utility.PluginsConfigurationUtilityApplication
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.exception.ExternalApplicationException
import ru.fullrest.mfr.plugins_configuration_utility.javafx.TaskFactory
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.service.RestTemplateService
import ru.fullrest.mfr.plugins_configuration_utility.util.ifNotExists
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

class LauncherController : FxController() {

    @Autowired
    private lateinit var application: PluginsConfigurationUtilityApplication

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

    @Autowired
    private lateinit var restTemplateService: RestTemplateService

    @Autowired
    private lateinit var taskFactory: TaskFactory

    @FXML
    private lateinit var updateButton: VBox

    @FXML
    private lateinit var updateText: Button

    @FXML
    private lateinit var refreshSymbol: Label

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

    @FXML
    private lateinit var lava: VBox

    private var listVersionsForUpdate: MutableList<String> = mutableListOf()
    private val animation: AtomicBoolean = AtomicBoolean(false)

    override fun init() {
        stage.onShowing = EventHandler {
            gameVersion.text = applicationProperties.gameVersion
            applicationVersion.text = applicationProperties.applicationVersion
            gamePath.text = files.gameFolder.absolutePath
            gamePath.tooltip = Tooltip(gamePath.text)
            files.morrowind.ifNotExists { disableButtonWithTooltip(playMge, "Файл Morrowind.exe не найден") }
            files.launcher.ifNotExists { disableButtonWithTooltip(launcher, "Файл Morrowind Launcher.exe не найден") }
            files.mcp.ifNotExists { disableButtonWithTooltip(mcp, "Файл Morrowind Code Patch.exe не найден") }
            files.mge.ifNotExists { disableButtonWithTooltip(mge, "Файл MGEXEgui.exe не найден") }
            files.openMw.ifNotExists { disableButtonWithTooltip(openMw, "Файл openmw.exe не найден") }
            files.openMwLauncher.ifNotExists {
                disableButtonWithTooltip(openMwLauncher, "Файл openmw-launcher.exe не найден")
            }
            updateText.text = UPDATE_NOT_FOUND_TEXT
            refreshSymbol.isVisible = true
            updateButton.styleClass.remove(UPDATE_FOUND_STYLE)
            checkUpdate()
        }

        betaLabel.isVisible = applicationProperties.beta

    }

    fun startGame() {
        try {
            Runtime.getRuntime().exec("\"${files.morrowind}\"", null, files.gameFolder)
            exitProcess(0)
        } catch (e: IOException) {
            throw ExternalApplicationException("Невозможно запустить Morrowind", e)
        }
    }

    fun startOpenMw() {
        try {
            Runtime.getRuntime().exec("\"${files.openMw}\"", null, files.gameFolder)
            Platform.exit()
        } catch (e: IOException) {
            throw ExternalApplicationException("Невозможно запустить Open Morrowind")
        }
    }

    fun startOpenMwLauncher() {
        try {
            Runtime.getRuntime().exec("\"${files.openMwLauncher}\"", null, files.gameFolder)
            Platform.exit()
        } catch (e: IOException) {
            throw ExternalApplicationException("Невозможно запустить Open Morrowind Launcher")
        }
    }

    fun startLauncher() {
        try {
            Runtime.getRuntime().exec("\"${files.launcher}\"", null, files.gameFolder)
//            Platform.exit()
        } catch (e: IOException) {
            throw ExternalApplicationException("Невозможно запустить Morrowind Launcher")
        }
    }

    fun startMCP() {
        ProcessBuilder("\"${files.mcp}\"").directory(files.gameFolder).start()
//        Runtime.getRuntime().exec("\"${files.mcp}\"", null, files.gameFolder)
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

    fun openDiscord() {
        application.hostServices.showDocument(applicationProperties.discordLink)
    }

    fun openYoutube() {
        application.hostServices.showDocument(applicationProperties.youtubeLink)
    }

    fun openVk() {
        application.hostServices.showDocument(applicationProperties.vkLink)
    }

    fun updateButtonClick() {
        if (updateButton.styleClass.contains(UPDATE_FOUND_STYLE)) {
            startUpdate()
        } else {
            checkUpdate()
        }
    }

    fun openConfiguration() {
        hide()
        pluginConfigurationController.show()
    }

    private fun disableButtonWithTooltip(button: Button, text: String) {
        button.tooltip = Tooltip(text).also {
            it.showDelay = Duration.seconds(0.3)
        }
        button.onAction = EventHandler { obj: ActionEvent -> obj.consume() }
        button.opacity = 0.7
    }

    private fun checkUpdate() {
        launch {
            val rotation = launch {
                val rotateTransition = RotateTransition(Duration.millis(500.0), refreshSymbol)
                rotateTransition.byAngle = 360.0
                while (true) {
                    rotateTransition.play()
                    delay(500)
                }
            }
            val listOfVersion = withContext(Dispatchers.Default) {
                restTemplateService.exchange(
                    link = Links.GAME_VERSION_HISTORY,
                    clazz = ArrayList<String>().javaClass
                )
            }
            listOfVersion?.last()?.also {
                val indexCurrentVersion = listOfVersion.indexOf(applicationProperties.gameVersion)
                if (it != applicationProperties.gameVersion && indexCurrentVersion >= 0) {
                    updateButton.styleClass.add(UPDATE_FOUND_STYLE)
                    updateText.text = UPDATE_FOUND_TEXT
                    refreshSymbol.isVisible = false
                    listVersionsForUpdate.clear()
                    listVersionsForUpdate.addAll(listOfVersion.drop(indexCurrentVersion + 1))
                    updateAnimation()
                    createAlert(stage).info(description = "Доступно обновление!")
                }
            }
            rotation.cancel()
        }
    }

    private fun startUpdate() {
        launch {
            val task = taskFactory.getGameUpdateTask()
            task.listVersions = listVersionsForUpdate
            task.run()
            applicationProperties.initVersion()
            gameVersion.text = applicationProperties.gameVersion
            updateButton.styleClass.remove(UPDATE_FOUND_STYLE)
            updateText.text = UPDATE_NOT_FOUND_TEXT
            refreshSymbol.isVisible = true
            listVersionsForUpdate.clear()
            files.updateEsmFileChangeDate()
        }
    }

    private fun updateAnimation(){
        if (animation.getAndSet(true).not()) {
            val brightness = listOf(
                1.0, 0.95, 0.9, 0.85, 0.8, 0.75, 0.7,
                0.65, 0.6, 0.55, 0.5, 0.45, 0.4, 0.35,
                0.3, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55,
                0.6, 0.65, 0.7, 0.85, 0.9, 0.95, 1.0
            )
            launch {
                while (true) {
                    brightness.forEach {
                        lava.opacity = it
                        delay(25)
                    }
                }
            }
        }
    }

    fun close() {
        Platform.exit()
    }

    companion object {
        private const val UPDATE_FOUND_STYLE = "found"
        private const val UPDATE_FOUND_TEXT = "   Обновить"
        private const val UPDATE_NOT_FOUND_TEXT = "Обновления"
    }
}