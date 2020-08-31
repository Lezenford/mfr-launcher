package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.service.FileService
import java.io.IOException
import java.util.*

class MgeConfigurationController : FxController() {

    @Autowired
    private lateinit var fileService: FileService

    @Autowired
    private lateinit var files: ApplicationFiles

    @Autowired
    private lateinit var alertController: AlertController

    @FXML
    private lateinit var highPerformanceButton: ToggleButton

    @FXML
    private lateinit var middlePerformanceButton: ToggleButton

    @FXML
    private lateinit var lowPerformanceButton: ToggleButton

    @FXML
    private lateinit var necroPerformanceButton: ToggleButton

    @FXML
    private lateinit var customPerformanceButton: ToggleButton

    private val toggleGroup = ToggleGroup()

    override fun init() {
        highPerformanceButton.toggleGroup = toggleGroup
        middlePerformanceButton.toggleGroup = toggleGroup
        lowPerformanceButton.toggleGroup = toggleGroup
        necroPerformanceButton.toggleGroup = toggleGroup
        customPerformanceButton.toggleGroup = toggleGroup

        stage.onShowing = EventHandler { checkPresetsButtons() }

        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event: KeyEvent ->
            if (event.code == KeyCode.ESCAPE) {
                hide()
            }
        }
    }

    fun startMGE() {
        try {
            val processHandle = ProcessBuilder("\"${files.mge.absolutePath}\"").start().toHandle()
            CoroutineScope(Dispatchers.JavaFx).launch {
                while (processHandle.isAlive) {
                    delay(100)
                }
                checkPresetsButtons()
            }
        } catch (e: IOException) {
            launch {
                alertController.error(
                    title = "Невозможно запустить Morrowind Graphics Extender!",
                    exception = e,
                    closeButtonEvent = EventHandler { alertController.hide() }
                )
            }
        }
    }

    fun useHighPerformanceConfig() {
        if (checkMGEFilesForUnique()) {
            fileService.saveMGEBackup()
        }
        fileService.setMGEConfig(files.highPerformanceMge)
        checkPresetsButtons()
    }

    fun useMiddlePerformanceConfig() {
        if (checkMGEFilesForUnique()) {
            fileService.saveMGEBackup()
        }
        fileService.setMGEConfig(files.middlePerformanceMge)
        checkPresetsButtons()
    }

    fun useLowPerformanceConfig() {
        if (checkMGEFilesForUnique()) {
            fileService.saveMGEBackup()
        }
        fileService.setMGEConfig(files.lowPerformanceMge)
        checkPresetsButtons()
    }

    fun useNecroPerformanceConfig() {
        if (checkMGEFilesForUnique()) {
            fileService.saveMGEBackup()
        }
        fileService.setMGEConfig(files.necroPerformanceMge)
        checkPresetsButtons()
    }

    fun useCustomPerformanceConfig() {
        fileService.setMGEConfig(files.mgeBackupConfig)
        checkPresetsButtons()
    }

    private fun checkMGEFilesForUnique(): Boolean {
        val top = fileService.getFileMD5(files.highPerformanceMge)
        val middle = fileService.getFileMD5(files.middlePerformanceMge)
        val low = fileService.getFileMD5(files.lowPerformanceMge)
        val necro = fileService.getFileMD5(files.necroPerformanceMge)
        val current = fileService.getFileMD5(files.mgeConfig)
        return (!Arrays.equals(current, top) && !Arrays.equals(current, middle)
                && !Arrays.equals(current, low) && !Arrays.equals(current, necro))
    }

    private fun checkPresetsButtons() {
        val mgeConfigMd5 = fileService.getFileMD5(files.mgeConfig)
        val mgeBackupConfigMd5 = fileService.getFileMD5(files.mgeBackupConfig)
        val highMgeConfigMd5 = fileService.getFileMD5(files.highPerformanceMge)
        val middleMgeConfigMd5 = fileService.getFileMD5(files.middlePerformanceMge)
        val lowMgeConfigMd5 = fileService.getFileMD5(files.lowPerformanceMge)
        val necroMgeConfigMd5 = fileService.getFileMD5(files.necroPerformanceMge)
        if (Arrays.equals(mgeConfigMd5, highMgeConfigMd5)) {
            toggleGroup.selectToggle(highPerformanceButton)
        } else {
            if (Arrays.equals(mgeConfigMd5, middleMgeConfigMd5)) {
                toggleGroup.selectToggle(middlePerformanceButton)
            } else {
                if (Arrays.equals(mgeConfigMd5, lowMgeConfigMd5)) {
                    toggleGroup.selectToggle(lowPerformanceButton)
                } else {
                    if (Arrays.equals(mgeConfigMd5, necroMgeConfigMd5)) {
                        toggleGroup.selectToggle(necroPerformanceButton)
                    } else {
                        if (mgeConfigMd5 != null) {
                            toggleGroup.selectToggle(customPerformanceButton)
                        } else {
                            toggleGroup.selectToggle(null)
                        }
                    }
                }
            }
        }
        customPerformanceButton.isDisable = !customPerformanceButton.isSelected && mgeBackupConfigMd5 == null
    }
}