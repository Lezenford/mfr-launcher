package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import org.springframework.beans.factory.annotation.Autowired
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.service.FileService
import java.io.File
import java.util.*

class OpenMwConfigurationController : FxController() {
    @Autowired
    private lateinit var files: ApplicationFiles

    @Autowired
    private lateinit var properties: ApplicationProperties

    @Autowired
    private lateinit var fileService: FileService

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
    }

    fun useHighPerformanceConfig() {
        if (!compareMD5ForConfigurations(files.highPerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.middlePerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.lowPerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.necroPerformanceOpenMwFolder)
        ) {
            fileService.saveOpenMwBackupConfig()
        }
        fileService.setOpenMwConfig(files.highPerformanceOpenMwFolder)
        checkPresetsButtons()
    }

    fun useMiddlePerformanceConfig() {
        if (!compareMD5ForConfigurations(files.highPerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.middlePerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.lowPerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.necroPerformanceOpenMwFolder)
        ) {
            fileService.saveOpenMwBackupConfig()
        }
        fileService.setOpenMwConfig(files.middlePerformanceOpenMwFolder)
        checkPresetsButtons()
    }

    fun useLowPerformanceConfig() {
        if (!compareMD5ForConfigurations(files.highPerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.middlePerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.lowPerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.necroPerformanceOpenMwFolder)
        ) {
            fileService.saveOpenMwBackupConfig()
        }
        fileService.setOpenMwConfig(files.lowPerformanceOpenMwFolder)
        checkPresetsButtons()
    }

    fun useNecroPerformanceConfig() {
        if (!compareMD5ForConfigurations(files.highPerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.middlePerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.lowPerformanceOpenMwFolder) &&
            !compareMD5ForConfigurations(files.necroPerformanceOpenMwFolder)
        ) {
            fileService.saveOpenMwBackupConfig()
        }
        fileService.setOpenMwConfig(files.necroPerformanceOpenMwFolder)
        checkPresetsButtons()
    }

    fun useCustomPerformanceConfig() {
        fileService.setOpenMwConfig(files.openMwBackupConfigFolder)
        checkPresetsButtons()
    }

    private fun checkPresetsButtons() {
        if (compareMD5ForConfigurations(files.highPerformanceOpenMwFolder)) {
            toggleGroup.selectToggle(highPerformanceButton)
        } else {
            if (compareMD5ForConfigurations(files.middlePerformanceOpenMwFolder)) {
                toggleGroup.selectToggle(middlePerformanceButton)
            } else {
                if (compareMD5ForConfigurations(files.lowPerformanceOpenMwFolder)) {
                    toggleGroup.selectToggle(lowPerformanceButton)
                } else {
                    if (compareMD5ForConfigurations(files.necroPerformanceOpenMwFolder)) {
                        toggleGroup.selectToggle(necroPerformanceButton)
                    } else {
                        if (files.openMwConfigFolder.exists() && files.openMwConfigFolder.listFiles() != null &&
                            Arrays.stream(Objects.requireNonNull(files.openMwConfigFolder.listFiles()))
                                .map { obj: File -> obj.name }
                                .allMatch { o: String? -> properties.openMwConfigFiles.contains(o) }
                        ) {
                            toggleGroup.selectToggle(customPerformanceButton)
                        } else {
                            toggleGroup.selectToggle(null)
                        }
                    }
                }
            }
        }
        customPerformanceButton.isDisable = !customPerformanceButton.isSelected &&
                (files.openMwBackupConfigFolder.listFiles() == null ||
                        !Arrays.stream(Objects.requireNonNull(files.openMwBackupConfigFolder.listFiles()))
                            .map { obj: File -> obj.name }
                            .allMatch { o: String? -> properties.openMwConfigFiles.contains(o) })
    }

    private fun compareMD5ForConfigurations(configuration: File): Boolean {
        val openMwConfigFolder = files.openMwConfigFolder
        val currentConfigurationFiles = openMwConfigFolder.listFiles()
        val configurationFiles = configuration.listFiles()
        return if (currentConfigurationFiles != null && configurationFiles != null) {
            for (fileName in properties.openMwConfigFiles) {
                val currentConfigFile = Arrays.stream(currentConfigurationFiles)
                    .filter { file: File -> file.name.equals(fileName, ignoreCase = true) }.findFirst()
                val configFile = Arrays.stream(configurationFiles)
                    .filter { file: File -> file.name.equals(fileName, ignoreCase = true) }.findFirst()
                if (currentConfigFile.isPresent && configFile.isPresent) {
                    val currentConfigFileMd5 = fileService.getFileMD5(currentConfigFile.get())
                    val configFileMd5 = fileService.getFileMD5(configFile.get())
                    if (!Arrays.equals(currentConfigFileMd5, configFileMd5)) {
                        return false
                    }
                } else {
                    return false
                }
            }
            true
        } else {
            false
        }
    }
}