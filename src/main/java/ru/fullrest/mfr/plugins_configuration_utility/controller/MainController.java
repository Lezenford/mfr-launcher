package ru.fullrest.mfr.plugins_configuration_utility.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.PluginsConfigurationUtilityApplication;
import ru.fullrest.mfr.plugins_configuration_utility.config.ConfigurationControllers;
import ru.fullrest.mfr.plugins_configuration_utility.config.StageControllers;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository;

import java.io.File;
import java.io.IOException;

/**
 * Created on 01.11.2018
 *
 * @author Alexey Plekhanov
 */
@Log4j2
public class MainController extends AbstractController {

    // Инъекции Spring
    @Autowired
    private PropertiesRepository propertiesRepository;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private PluginsConfigurationUtilityApplication application;

    @Autowired
    private StageControllers stageControllers;

    @Autowired
    private ConfigurationControllers configurationControllers;

    @FXML
    @Getter
    private Label version;

    @FXML
    @Getter
    private TextField gamePath;

    public void open() {
        fileManager.initGameDirectory();
        gamePath.setText(propertiesRepository.findByKey(PropertyKey.GAME_DIRECTORY_PATH).getValue());
        fileManager.checkVersion();
    }

    public void startGame() {
        try {
            Runtime.getRuntime().exec(fileManager.getAbsolutePath(FileManager.MORROWIND_EXE), null,
                    new File(fileManager.getGamePath()));
            System.exit(0);
        } catch (IOException e) {
            createAlertForException(e, "Невозможно запустить Morrowind!");
        }
    }

    public void startLauncher() {
        try {
            Runtime.getRuntime().exec(fileManager.getAbsolutePath(FileManager.MORROWIND_LAUNCHER_EXE), null,
                    new File(fileManager.getGamePath()));
            System.exit(0);
        } catch (IOException e) {
            createAlertForException(e, "Невозможно запустить Morrowind Launcher!");
        }
    }

    public void startMCP() {
        try {
            Runtime.getRuntime().exec(fileManager.getAbsolutePath(FileManager.MORROWIND_MCP_EXE), null,
                    new File(fileManager.getGamePath()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка!");
            if (e.getMessage().contains("error=740")) {
                alert.setHeaderText("Недостаточно прав для запуска Morrowind Code Patch!");
                alert.setContentText("Перезапустите конфигуратор от имени администратора");
            } else {
                alert.setHeaderText("Невозможно запустить Morrowind Code Patch!");
                alert.setContentText(e.getMessage());
            }
            alert.showAndWait();
        }
    }

    public void startMGE() {
        try {
            configurationControllers.getMGEConfigurationView().getController().beforeOpen();
            stageControllers.getMgeConfigurationStage().show();
        } catch (IOException e) {
            log.error(e);
        }
    }

    public void openForum() {
        application.getHostServices().showDocument(propertiesRepository.findByKey(PropertyKey.FORUM_LINK).getValue());
    }

    public void openReadme() {
        application.getHostServices().showDocument(fileManager.getAbsolutePath(FileManager.MORROWIND_README));
    }

    public void openConfiguration() throws IOException {
        configurationControllers.getConfigurationView().getController().beforeOpen();
        stageControllers.getApplicationStage().hide();
        stageControllers.getPluginConfigurationStage().show();
    }

    private void createAlertForException(Throwable e, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка!");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }
}
