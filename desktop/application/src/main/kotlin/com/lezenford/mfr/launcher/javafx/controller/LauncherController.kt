package com.lezenford.mfr.launcher.javafx.controller

import com.dustinredmond.fxtrayicon.FXTrayIcon
import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.javafx.component.FxController
import com.lezenford.mfr.javafx.extensions.runFx
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.extension.listener
import com.lezenford.mfr.launcher.extension.versionLine
import com.lezenford.mfr.launcher.javafx.component.ProgressComponent
import com.lezenford.mfr.launcher.javafx.component.ProgressComponent.Status
import com.lezenford.mfr.launcher.model.entity.Properties
import com.lezenford.mfr.launcher.service.Location
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.model.PropertiesService
import com.lezenford.mfr.launcher.service.factory.FxControllerFactory
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.runner.RunnerService
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.RadioButton
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.ToggleButton
import javafx.scene.control.Tooltip
import javafx.scene.layout.VBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess

@Profile("GUI")
@Component
class LauncherController(
    private val taskFactory: TaskFactory,
    private val fxControllerFactory: FxControllerFactory,
    private val runnerService: RunnerService,
    private val propertiesService: PropertiesService,
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
    private val ruRegion: RadioButton by fxml()
    private val euRegion: RadioButton by fxml()
    private val buildComboBox: ComboBox<String> by fxml()

    private val progressBarComponent = ProgressComponent(fxmlLoader, taskFactory) { line ->
        askLineSwitch(line, dismissOnRefuse = true)
    }
    private val buildBoxUpdating = AtomicBoolean(false)

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
                refreshStatus()
            }

            consistencyCheckButton.isDisable = enable.not()
            consistencyCheckButton.tooltip = Tooltip("offline mod")
        }

        onlineModButton.onMouseClicked = EventHandler {
            State.onlineMode.tryEmit(State.onlineMode.value.not())
        }

        State.launcherUpdateStatus.listener { refreshStatus() }
        State.gameUpdateStatus.listener { refreshStatus() }
        State.newLineAvailable.listener { refreshStatus() }

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

        launch {
            State.availableRuLocation.collect { ruRegion.isDisable = !it }
        }

        launch {
            State.availableEuLocation.collect { euRegion.isDisable = !it }
        }

        launch {
            State.location.collect {
                when (it) {
                    Location.RU -> ruRegion.isSelected = true
                    Location.EU -> euRegion.isSelected = true
                }
            }
        }

        ruRegion.selectedProperty().addListener { _, _, newValue ->
            if (newValue) {
                launch { State.location.emit(Location.RU) }
                euRegion.isSelected = false
            }
        }

        euRegion.selectedProperty().addListener { _, _, newValue ->
            if (newValue) {
                launch { State.location.emit(Location.EU) }
                ruRegion.isSelected = false
            }
        }

        launch {
            State.availableBuilds.collect { builds ->
                withBuildBoxUpdating {
                    buildComboBox.items.setAll(builds)
                    buildComboBox.value = State.selectedBuild.value
                }
            }
        }
        launch {
            State.selectedBuild.collect { line -> withBuildBoxUpdating { buildComboBox.value = line } }
        }
        launch { State.gameInstalled.collect { refreshBuildBoxAvailability() } }
        launch { State.serverConnection.collect { refreshBuildBoxAvailability() } }
        buildComboBox.valueProperty().addListener { _, _, chosen ->
            if (buildBoxUpdating.get() || chosen == null || chosen == State.selectedBuild.value) {
                return@addListener
            }
            launch {
                try {
                    if (askLineSwitch(chosen, dismissOnRefuse = false)) {
                        launcherTab.tabPane.selectionModel.select(launcherTab)
                        progressBarComponent.executeTask(taskFactory.gameUpdateTask())
                    } else {
                        withBuildBoxUpdating { buildComboBox.value = State.selectedBuild.value }
                    }
                } finally {
                    refreshStatus()
                }
            }
        }
    }

    /**
     * Единственное место, где из состояния выводится надпись на кнопке обновления.
     * Порядок — приоритет: лаунчер обновляется раньше игры, установка раньше обновления,
     * обновление в своей линии раньше предложения сменить линию.
     */
    private suspend fun refreshStatus() {
        if (State.onlineMode.value.not()) return
        val status = when {
            State.launcherUpdateStatus.value.needUpdate() -> Status.LAUNCHER_UPDATE
            State.gameInstalled.value.not() -> Status.GAME_INSTALL
            State.gameUpdateStatus.value.needUpdate() -> Status.GAME_UPDATE
            State.newLineAvailable.value != null -> Status.NEW_LINE
            else -> Status.DISABLE
        }
        progressBarComponent.setStatus(status)
    }

    /**
     * Диалог перехода на линию. Согласие сразу делает линию выбранной, отказ по подсветке
     * прячет предложение до появления следующей линии.
     */
    private suspend fun askLineSwitch(line: String, dismissOnRefuse: Boolean): Boolean {
        val current = State.selectedBuild.value ?: State.schema.value?.version?.versionLine() ?: "не определена"
        val description = buildString {
            appendLine("Линия игры $line (сейчас установлена $current).")
            appendLine("Переход скачает изменившиеся файлы игры. Сохранения другой линии могут оказаться несовместимы.")
            if (dismissOnRefuse) {
                appendLine("«Отмена» скроет это предложение до появления следующей линии.")
            }
            append("Перейти сейчас?")
        }
        val agreed = fxControllerFactory.controller<QuestionController>().show(
            title = "Смена линии игры",
            description = description
        )
        withContext(Dispatchers.IO) {
            if (agreed) {
                State.selectedBuild.emit(line)
                State.newLineAvailable.emit(null)
            } else if (dismissOnRefuse) {
                propertiesService.updateValue(Properties.Key.DISMISSED_BUILD, line)
                State.newLineAvailable.emit(null)
            }
        }
        return agreed
    }

    private fun refreshBuildBoxAvailability() {
        buildComboBox.isDisable = (State.gameInstalled.value && State.serverConnection.value).not()
    }

    private inline fun withBuildBoxUpdating(block: () -> Unit) {
        buildBoxUpdating.set(true)
        try {
            block()
        } finally {
            buildBoxUpdating.set(false)
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

    fun telegram() {
        runnerService.openTelegram()
    }

    fun website() {
        runnerService.openWebsite()
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
                refreshStatus()
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