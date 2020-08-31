package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.RestTemplate
import ru.fullrest.mfr.plugins_configuration_utility.javafx.TaskFactory
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Properties
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository
import kotlin.system.exitProcess

class GameInstallController : AbstractProgressController() {

    @FXML
    private lateinit var prepareBox: VBox

    @FXML
    private lateinit var progressBarBox: VBox

    @FXML
    private lateinit var escapeButton: Button

    @FXML
    private lateinit var escapeButtonHover: HBox

    @FXML
    private lateinit var text: Label

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var taskFactory: TaskFactory

    @Autowired
    private lateinit var propertiesRepository: PropertiesRepository

    @Autowired
    private lateinit var insertKeyController: InsertKeyController

    override fun init() {
        closeButton.visibleProperty().addListener { _, _, visible: Boolean? ->
            visible?.also {
                escapeButton.isVisible = !it
            }
        }
        prepareBox.visibleProperty().addListener { _, _, visible: Boolean? ->
            visible?.also {
                progressBarBox.isVisible = !it
                escapeButton.isVisible = !it
            }
        }
        progressBarBox.visibleProperty().addListener { _, _, visible: Boolean? ->
            visible?.also {
                updateProgress(0, 0)
                closeButton.isVisible = false
                setDescription("")
            }
        }
        stage.onShowing = EventHandler {
            prepareBox.isVisible = true
        }
        escapeButton.visibleProperty().addListener { _, _, visible: Boolean? ->
            visible?.also {
                escapeButtonHover.isVisible = visible
            }
        }
        val keyCombination = KeyCodeCombination(KeyCode.B, KeyCombination.CONTROL_DOWN)
        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
            if (keyCombination.match(event)) {
                insertKeyController.showAndWait()
                exitProcess(0)
            }
        }
    }

    fun showAndWaitUpdateLauncher() {
        downloadGame = false
        text.text = "Доступно обновление для M[FR] Launcher. Хотите установить?"
        showAndWait()
    }

    fun showAndWaitDownloadGame() {
        downloadGame = true
        text.text = "M[FR] доступен для скачивания. Хотите установить?"
        showAndWait()
    }

    private var downloadGame: Boolean = true

    fun accept() = launch {
        prepareBox.isVisible = false
        val task = if (downloadGame) {
            taskFactory.getGameInstallTask()
        } else {
            taskFactory.getLauncherUpdateTask()
        }
        val installed = runAsync(task).also { job ->
            escapeButton.onAction = EventHandler {
                job.cancel()
                exitProcess(0)
            }
        }.await()
        closeButton.isVisible = true
        if (installed) {
            propertiesRepository.findByKey(PropertyKey.INSTALLED)
                ?: propertiesRepository.save(Properties(key = PropertyKey.INSTALLED))
        }
    }

    fun decline() {
        exitProcess(0)
    }

    fun finish() {
        hide()
    }

    override val progressBarMinWidth: Double = PROGRESS_BAR_MIN_WIDTH
    override val progressBarMaxWidth: Double = PROGRESS_BAR_MAX_WIDTH

    companion object {
        private const val PROGRESS_BAR_MIN_WIDTH = 20.0
        private const val PROGRESS_BAR_MAX_WIDTH = 490.0
    }
}