package ru.fullrest.mfr.launcher.javafx.controller

import com.dustinredmond.fxtrayicon.FXTrayIcon
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.ToggleButton
import javafx.scene.layout.VBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.javafx.component.FxController
import ru.fullrest.mfr.launcher.Launcher
import ru.fullrest.mfr.launcher.component.ApplicationStatus
import ru.fullrest.mfr.launcher.component.EventPublisher
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.config.properties.GameProperties
import ru.fullrest.mfr.launcher.javafx.TaskFactory
import ru.fullrest.mfr.launcher.javafx.component.UpdateComponent
import ru.fullrest.mfr.launcher.service.RestTemplateService
import kotlin.io.path.absolutePathString
import kotlin.io.path.pathString
import kotlin.io.path.readLines
import kotlin.system.exitProcess

@Component
class LauncherController(
    private val application: Launcher,
    private val applicationProperties: ApplicationProperties,
    private val gameProperties: GameProperties,
    private val publisher: EventPublisher,
    private val applicationStatus: ApplicationStatus,
    private val taskFactory: TaskFactory,
    private val restTemplateService: RestTemplateService
) : FxController("fxml/main.fxml") {
    private val modeTabPane: TabPane by fxml()
    private val launcherTab: Tab by fxml()
    private val settingTab: Tab by fxml()
    private val gamePath: Label by fxml()
    private val launcherPath: Label by fxml()
    private val version: Label by fxml()
    private val launcherVersion: Label by fxml()
    private val classicButtons: VBox by fxml()
    private val openMwButtons: VBox by fxml()
    private val gameSettingButton: Button by fxml()
    private val consistencyCheckButton: Button by fxml()
    private val onlineModButton: ToggleButton by fxml()

    private val updateComponent = UpdateComponent(fxmlLoader, taskFactory)

    init {
        if (FXTrayIcon.isSupported()) {
            FXTrayIcon(stage, javaClass.classLoader.getResource("icon.png")).apply {
                show()
                addExitItem(true)
            }
        } else {
            log.error("Tray icon is not supported on this platform")
        }

        launcherTab.selectedProperty().addListener { _, _, value ->
            launcherTab.isDisable = value.not()
            settingTab.isDisable = value
        }

        gamePath.text = applicationProperties.gameFolder.absolutePathString()
        launcherPath.text = applicationProperties.gameFolder.parent.absolutePathString()
        launcherVersion.text = applicationProperties.version
        applicationStatus.gameVersion.addListener { launch { version.text = it } }
        applicationStatus.gameInstalled.addListener {
            launch {
                classicButtons.isDisable = it.not()
                openMwButtons.isDisable = it.not()
                gameSettingButton.isDisable = it.not()
                consistencyCheckButton.isDisable = it.not()
            }
            if (it) {
                updateComponent.setStatus(UpdateComponent.Status.DISABLE)
                applicationStatus.gameVersion.value =
                    gameProperties.versionFile.readLines().find { line -> line.isNotEmpty() } ?: ""
                checkUpdate()
            } else {
                updateComponent.setStatus(UpdateComponent.Status.INSTALL_READY)
            }
        }
        applicationStatus.onlineMode.addListener {
            onlineModButton.isSelected = it
            onlineModButton.text = if (it) {
                "Online-mod"
            } else {
                "Offline-mod"
            }
        }
        onlineModButton.onMouseClicked = EventHandler {
            applicationStatus.onlineMode.value = applicationStatus.onlineMode.value.not()
        }
    }

    fun classic() {
        ProcessBuilder("\"${gameProperties.classic.application.absolutePathString()}\"").also {
            it.directory(applicationProperties.gameFolder.toFile())
        }.start()
        hide()
    }

    fun classicLauncher() {
        ProcessBuilder("\"${gameProperties.classic.launcher.absolutePathString()}\"").also {
            it.directory(applicationProperties.gameFolder.toFile())
        }.start()
    }

    fun openMw() {
        ProcessBuilder("\"${gameProperties.openMw.application.absolutePathString()}\"").also {
            it.directory(applicationProperties.gameFolder.toFile())
        }.start()
    }

    fun openMwLauncher() {
        ProcessBuilder("\"${gameProperties.openMw.launcher.absolutePathString()}\"").also {
            it.directory(applicationProperties.gameFolder.toFile())
        }.start()
    }

    fun mcp() {
        ProcessBuilder("\"${gameProperties.classic.mcp.absolutePathString()}\"").also {
            it.directory(applicationProperties.gameFolder.toFile())
        }.start()
    }

    fun mge() {
        publisher.sendShowRequest(MgeController::class)
    }

    fun openMwConfig() {
        publisher.sendShowRequest(OpenMwController::class)
    }

    fun configureGame() {
        publisher.sendShowRequest(GameController::class)
        hide()
    }

    fun switchTab() {
        if (launcherTab.isSelected) {
            modeTabPane.selectionModel.select(settingTab)
        } else {
            modeTabPane.selectionModel.select(launcherTab)
        }
    }

    fun donation() {
        publisher.sendShowRequest(DonationController::class)
    }

    fun readme() {
        application.hostServices.showDocument(applicationProperties.readme.pathString)
    }

    fun forum() {
        application.hostServices.showDocument(applicationProperties.social.forum)
    }

    fun discord() {
        application.hostServices.showDocument(applicationProperties.social.discord)
    }

    fun youtube() {
        application.hostServices.showDocument(applicationProperties.social.youtube)
    }

    fun vk() {
        application.hostServices.showDocument(applicationProperties.social.vk)
    }

    fun patreon() {
        application.hostServices.showDocument(applicationProperties.social.patreon)
    }

    fun minimize() {
        hide()
    }

    fun checkConsistency() {
        launch(Dispatchers.Default) {
            withContext(Dispatchers.JavaFx) {
                launcherTab.tabPane.selectionModel.select(launcherTab)
                consistencyCheckButton.isDisable = true
            }
            updateComponent.setStatus(UpdateComponent.Status.DISABLE)
            try {
                taskFactory.checkGameConsistencyTask().execute(updateComponent.progressBar)
            } finally {
                consistencyCheckButton.isDisable = false
                checkUpdate()
            }
        }
    }

    fun exit() {
        exitProcess(0)
    }

    @Scheduled(fixedDelay = 900000L, initialDelay = 900000L)
    fun checkUpdate() {
        if (applicationStatus.onlineMode.value) {
            launch(Dispatchers.Default) {
                val result = taskFactory.checkGameUpdateTask().execute()
                if (result.isNotEmpty()) {
                    updateComponent.setStatus(UpdateComponent.Status.UPDATE_READY)
                }
                if (restTemplateService.client().version != applicationProperties.version) {
                    updateComponent.setStatus(UpdateComponent.Status.LAUNCHER_UPDATE_READY)
                }
            }
        }
    }

    companion object {
        private val log by Logger()
    }
}