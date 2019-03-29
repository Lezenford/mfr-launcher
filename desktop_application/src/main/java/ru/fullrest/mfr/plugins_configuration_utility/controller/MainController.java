package ru.fullrest.mfr.plugins_configuration_utility.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.PluginsConfigurationUtilityApplication;
import ru.fullrest.mfr.plugins_configuration_utility.config.PropertiesConfiguration;
import ru.fullrest.mfr.plugins_configuration_utility.javafx.View;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.manager.StageManager;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * Created on 01.11.2018
 *
 * @author Alexey Plekhanov
 */
@Log4j2
public class MainController implements AbstractController {

    @Autowired
    private FileManager fileManager;

    @Autowired
    private PluginsConfigurationUtilityApplication application;

    @Autowired
    private StageManager stageManager;

    @Autowired
    private View<MGEConfigurationController> mgeConfigurationView;

    @Autowired
    private View<PluginConfigurationController> pluginConfigurationView;

    @Autowired
    private View<ReadmeController> readmeView;

    @Autowired
    private View<HelpForProjectController> helpForProjectView;

    @Autowired
    private View<ProgressController> progressView;

    @Autowired
    private PropertiesConfiguration propertiesConfiguration;

    @FXML
    @Getter
    private Label version;

    @FXML
    @Getter
    private Label gamePath;

    public void startGame() {
        try {
            Runtime.getRuntime().exec(fileManager.getAbsolutePath(propertiesConfiguration.getMorrowind_exe()), null,
                    new File(fileManager.getGamePath(false)));
            System.exit(0);
        } catch (IOException e) {
            createAlertForException(e, "Невозможно запустить Morrowind!");
        }
    }

    public void startLauncher() {
        try {
            Runtime.getRuntime().exec(fileManager.getAbsolutePath(propertiesConfiguration.getLauncher_exe()), null,
                    new File(fileManager.getGamePath(false)));
            System.exit(0);
        } catch (IOException e) {
            createAlertForException(e, "Невозможно запустить Morrowind Launcher!");
        }
    }

    public void startMCP() {
        try {
            Runtime.getRuntime().exec(fileManager.getAbsolutePath(propertiesConfiguration.getMcp_exe()), null,
                    new File(fileManager.getGamePath(false)));
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
        mgeConfigurationView.getController().beforeOpen();
        stageManager.getMgeConfigurationStage().show();
    }

    public void openForum() {
        application.getHostServices().showDocument(propertiesConfiguration.getForumLink());
    }

    public void openReadme() {
        readmeView.getController().beforeOpen();
        stageManager.getApplicationStage().hide();
        stageManager.getReadmeStage().show();
    }

    public void helpForProject() {
        helpForProjectView.getController().beforeOpen();
        stageManager.getHelpForProjectStage().show();
    }

    public void checkUpdate() {
        progressView.getController().beforeOpen();
        progressView.getController().checkApplicationUpdate();
        stageManager.getProgressStage().showAndWait();
        progressView.getController().beforeOpen();
        progressView.getController().checkGameUpdate(propertiesConfiguration.getGameVersion(), false);
        stageManager.getProgressStage().showAndWait();
        String version = fileManager.checkVersion();
        propertiesConfiguration.setGameVersion(Objects.requireNonNullElse(version, ""));
        this.version.setText(version);

    }

    public void openConfiguration() {
        pluginConfigurationView.getController().beforeOpen();
        stageManager.getApplicationStage().hide();
        stageManager.getPluginConfigurationStage().show();
    }

    private void createAlertForException(Throwable e, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка!");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    public void close() {
        Platform.exit();
    }
}