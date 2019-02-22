package ru.fullrest.mfr.plugins_configuration_utility.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.config.PropertiesConfiguration;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.manager.StageManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Log4j2
public class MGEConfigurationController implements AbstractController {

    @Autowired
    private FileManager fileManager;

    @Autowired
    private PropertiesConfiguration propertiesConfiguration;

    @Autowired
    private StageManager stageManager;
    @FXML
    private ListView<MGEConfig> backups;
    @FXML
    private Button restoreBackupButton;

    public void init() {
        backups.getSelectionModel().selectionModeProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                restoreBackupButton.setDisable(false);
            } else {
                restoreBackupButton.setDisable(true);
            }
        });
    }

    @Override
    public void beforeOpen() {
        backups.getItems().removeAll(backups.getItems());
        List<File> mgeBackup = fileManager.getMGEBackup();
        if (mgeBackup != null) {
            for (File file : mgeBackup) {
                backups.getItems().add(new MGEConfig(file));
            }
        }
    }

    public void startMGE() {
        try {
            Runtime.getRuntime().exec(fileManager.getAbsolutePath(propertiesConfiguration.getMge_exe()), null,
                    new File(fileManager.getGamePath(false)));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка!");
            alert.setHeaderText("Невозможно запустить Morrowind Graphics Extender!");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void useHighPerformanceConfig() {
        if (fileManager.checkMGEFilesForUnique()) {
            saveConfig();
        }
        fileManager.setMGEConfig(fileManager.getOptionalPath(true) + "MGE\\top_PK\\MGE.ini");
    }

    public void useMiddlePerformanceConfig() {
        if (fileManager.checkMGEFilesForUnique()) {
            saveConfig();
        }
        fileManager.setMGEConfig(fileManager.getOptionalPath(true) + "MGE\\mid_PK\\MGE.ini");
    }

    public void useLowPerformanceConfig() {
        if (fileManager.checkMGEFilesForUnique()) {
            saveConfig();
        }
        fileManager.setMGEConfig(fileManager.getOptionalPath(true) + "MGE\\low_PK\\MGE.ini");
    }

    public void useNecroPerformanceConfig() {
        if (fileManager.checkMGEFilesForUnique()) {
            saveConfig();
        }
        fileManager.setMGEConfig(fileManager.getOptionalPath(true) + "MGE\\necro_PK\\MGE.ini");
    }

    public void saveConfig() {
        fileManager.saveMGEBackup();
        beforeOpen();
    }

    public void restoreBackup() {
        if (!backups.getSelectionModel().isEmpty()) {
            fileManager.setMGEConfig(backups.getSelectionModel().getSelectedItem().getConfig().getAbsolutePath());
        }
    }

    public void cancel() {
        stageManager.getMgeConfigurationStage().close();
    }

    @AllArgsConstructor
    private class MGEConfig {

        @Getter
        private File config;

        @Override
        public String toString() {
            return config.getAbsolutePath().substring(config.getAbsolutePath().length() - 21,
                    config.getAbsolutePath().length() - 4);
        }
    }
}
