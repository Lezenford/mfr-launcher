package com.lezenford.mfr.launcher.javafx.controller

import com.dustinredmond.fxtrayicon.FXTrayIcon
import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.javafx.component.FxController
import com.lezenford.mfr.javafx.extensions.runFx
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.extension.listener
import com.lezenford.mfr.launcher.javafx.component.ProgressComponent
import com.lezenford.mfr.launcher.javafx.component.ProgressComponent.Status
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.factory.FxControllerFactory
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.provider.NettyProvider
import com.lezenford.mfr.launcher.service.runner.RunnerService
import javafx.event.EventHandler
import javafx.scene.control.*
import javafx.scene.layout.VBox
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess

@Profile("GUI")
@Component
class LauncherController(
    private val taskFactory: TaskFactory,
    private val fxControllerFactory: FxControllerFactory,
    private val runnerService: RunnerService,
    nettyProvider: NettyProvider,
    applicationProperties: ApplicationProperties
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
    private val useTrayCheckbox: CheckBox by fxml()
    private val useLimitCheckbox: CheckBox by fxml()
    private val limitField: TextField by fxml()

    private val progressBarComponent = ProgressComponent(fxmlLoader, nettyProvider, taskFactory)

    init {
        if (FXTrayIcon.isSupported()) {
            FXTrayIcon(stage, javaClass.classLoader.getResource("icon.png")).apply {
                show()
                addMenuItem(
                    MenuItem("Показать").also {
                        it.onAction = EventHandler { runFx { this@LauncherController.show() } }
                    }
                )
                addMenuItem(
                    MenuItem("Выход").also {
                        it.onAction = EventHandler { exitProcess(0) }
                    }
                )
            }
        } else {
            log.error("Tray icon is not supported on this platform")
            launch { State.minimizeToTray.emit(false) }
            useTrayCheckbox.isDisable = true
        }

        State.onlineMode.listener(coroutineContext) { enable ->
            onlineModButton.isSelected = enable
            onlineModButton.text = if (enable) {
                "Online-mod"
            } else {
                "Offline-mod"
            }

            if (enable.not()) {
                progressBarComponent.setStatus(Status.DISABLE)
            }
            progressBarComponent.lock(enable.not())
            if (enable) {
                State.serverConnection.first { it }
                when {
                    State.launcherUpdateStatus.value.needUpdate() -> progressBarComponent.setStatus(Status.LAUNCHER_UPDATE)
                    State.gameInstalled.value.not() -> progressBarComponent.setStatus(Status.GAME_INSTALL)
                    State.gameUpdateStatus.value.needUpdate() -> progressBarComponent.setStatus(Status.GAME_UPDATE)
                }
            }

            consistencyCheckButton.isDisable = enable.not()
            consistencyCheckButton.tooltip = Tooltip("offline mod")
        }

        onlineModButton.onMouseClicked = EventHandler {
            State.onlineMode.tryEmit(State.onlineMode.value.not())
        }

        State.launcherUpdateStatus.listener {
            if (it.needUpdate()) {
                progressBarComponent.setStatus(Status.LAUNCHER_UPDATE)
            } else {
                if (State.gameUpdateStatus.value.needUpdate()) {
                    progressBarComponent.setStatus(Status.GAME_UPDATE)
                }
            }
        }

        State.gameUpdateStatus.listener {
            if (it.needUpdate() && State.launcherUpdateStatus.value.needUpdate().not() && State.gameInstalled.value) {
                progressBarComponent.setStatus(Status.GAME_UPDATE)
            }
        }

        launcherTab.selectedProperty().addListener { _, _, value ->
            launcherTab.isDisable = value.not()
            settingTab.isDisable = value
        }

        gamePath.text = applicationProperties.gameFolder.absolutePathString()
        launcherPath.text = applicationProperties.gameFolder.parent.absolutePathString()
        launcherVersion.text = applicationProperties.version
        State.gameVersion.listener(coroutineContext) { version.text = it }

        State.gameInstalled.listener(coroutineContext) {
            classicButtons.isDisable = it.not()
            openMwButtons.isDisable = it.not()
            gameSettingButton.isDisable = it.not()
            consistencyCheckButton.isDisable = it.not()
        }

        State.minimizeToTray.listener(coroutineContext) {
            useTrayCheckbox.isSelected = it
        }
        useTrayCheckbox.selectedProperty().addListener { _, _, newValue ->
            launch { State.minimizeToTray.emit(newValue) }
        }

        useLimitCheckbox.selectedProperty().addListener { _, _, newValue ->
            limitField.isDisable = newValue.not()
            if (newValue.not()) {
                limitField.text = "0"
            }
        }
        limitField.textFormatter = TextFormatter<Int> { if (it.text.matches(Regex("^\\d{0,7}$"))) it else null }
        limitField.textProperty().addListener { _, _, newValue ->
            if (newValue.matches(Regex("^\\d{0,7}$"))) {
                launch { State.speedLimit.emit(newValue?.toInt() ?: 0) }
            }
        }
        State.speedLimit.listener(coroutineContext) {
            useLimitCheckbox.isSelected = it > 0
            if (limitField.text != it.toString()) {
                limitField.text = it.toString()
            }
        }
    }

    fun classic() {
        runnerService.startClassicGame()
        minimize()
    }

    fun classicLauncher() {
        runnerService.startClassicLauncher()
    }

    fun openMw() {
        runnerService.startOpenMwGame()
        minimize()
    }

    fun openMwLauncher() {
        runnerService.startOpenMwLauncher()
    }

    fun mcp() {
        runnerService.startMcp()
    }

    fun mge() {
        fxControllerFactory.controller<MgeController>().show()
    }

    fun openMwConfig() {
        fxControllerFactory.controller<OpenMwController>().show()
    }

    fun configureGame() {
        fxControllerFactory.controller<GameController>().show()
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
        fxControllerFactory.controller<DonationController>().show()
    }

    fun readme() {
        runnerService.openReadme()
    }

    fun forum() {
        runnerService.openForum()
    }

    fun discord() {
        runnerService.openDiscord()
    }

    fun youtube() {
        runnerService.openYoutube()
    }

    fun vk() {
        runnerService.openVk()
    }

    fun patreon() {
        runnerService.openPatreon()
    }

    fun minimize() {
        if (State.minimizeToTray.value) {
            hide()
        } else {
            stage.isIconified = true
        }
    }

    fun checkConsistency() {
        launch {
            try {
                launcherTab.tabPane.selectionModel.select(launcherTab)
                consistencyCheckButton.isDisable = true
                progressBarComponent.executeTask(taskFactory.checkGameConsistencyTask())
            } finally {
                consistencyCheckButton.isDisable = false
                if (State.launcherUpdateStatus.value.needUpdate()) {
                    progressBarComponent.setStatus(Status.LAUNCHER_UPDATE)
                } else {
                    if (State.gameUpdateStatus.value.needUpdate()) {
                        progressBarComponent.setStatus(Status.GAME_UPDATE)
                    }
                }
            }
        }
    }

    fun exit() {
        exitProcess(0)
    }

    companion object {
        private val log by Logger()
    }
}