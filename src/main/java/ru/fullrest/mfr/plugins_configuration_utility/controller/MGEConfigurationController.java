package ru.fullrest.mfr.plugins_configuration_utility.controller;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.config.StageControllers;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MGEConfigurationController extends AbstractController {

    @Autowired
    private FileManager fileManager;

    @Autowired
    private StageControllers stageControllers;

    @Override
    public void init() {
        backups.getSelectionModel().selectionModeProperty().addListener(new ChangeListener<SelectionMode>() {
            @Override
            public void changed(ObservableValue<? extends SelectionMode> observable, SelectionMode oldValue,
                                SelectionMode newValue) {
                if (newValue != null) {
                    restoreBackupButton.setDisable(false);
                } else {
                    restoreBackupButton.setDisable(true);
                }
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

    @FXML
    private ListView<MGEConfig> backups;

    @FXML
    private Button restoreBackupButton;

    public void startMGE() {
        try {
            Runtime.getRuntime().exec(fileManager.getAbsolutePath(FileManager.MORROWIND_MGE_EXE), null,
                    new File(fileManager.getGamePath()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка!");
            alert.setHeaderText("Невозможно запустить Morrowind Graphics Extender!");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void useHightPerformanceConfig() {
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
        fileManager.setMGEConfig(backups.getSelectionModel().getSelectedItem().getConfig().getAbsolutePath());
    }

    public void cancel() throws IOException {
        stageControllers.getMgeConfigurationStage().hide();
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
