package ru.fullrest.mfr.plugins_configuration_utility.config;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Log4j2
@Configuration
public class StageControllers {

    @Autowired
    private ConfigurationControllers configurationControllers;

    @Value("${ui.title:M[FR] Настройки репака}")
    private String windowTitle;

    @Getter
    private Stage applicationStage;

    public void setApplicationStage(Stage applicationStage) throws IOException {
        this.applicationStage = applicationStage;
        applicationStage.setTitle(windowTitle);
        applicationStage.setScene(new Scene(configurationControllers.getMainView().getView()));
        applicationStage.getScene().getStylesheets().add("javafx/css/style.css");
        applicationStage.setResizable(false);
        applicationStage.centerOnScreen();
        applicationStage.getIcons().add(new Image("javafx/css/image/icon.png"));
    }

    private Stage pluginConfigurationStage;

    public Stage getPluginConfigurationStage() throws IOException {
        if (pluginConfigurationStage == null) {
            pluginConfigurationStage = new Stage();
            pluginConfigurationStage.setTitle("Конфигурация");
            pluginConfigurationStage.setScene(new Scene(configurationControllers.getConfigurationView().getView()));
            pluginConfigurationStage.getScene().getStylesheets().add("javafx/css/style.css");
            pluginConfigurationStage.setResizable(false);
            pluginConfigurationStage.initOwner(applicationStage);
            pluginConfigurationStage.initModality(Modality.APPLICATION_MODAL);
            pluginConfigurationStage.getIcons().add(new Image("javafx/css/image/icon.png"));
            pluginConfigurationStage.centerOnScreen();
            pluginConfigurationStage.setOnHiding(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    getApplicationStage().show();
                }
            });
        }
        return pluginConfigurationStage;
    }

    private Stage progressStage;

    public Stage getProgressStage() throws IOException {
        if (progressStage == null) {
            progressStage = new Stage();
            progressStage.setTitle("Пожалуйста, подождите...");
            progressStage.setScene(new Scene(configurationControllers.getProgressView().getView()));
            progressStage.setResizable(false);
            progressStage.initOwner(applicationStage);
            progressStage.initModality(Modality.APPLICATION_MODAL);
            progressStage.centerOnScreen();
            progressStage.getIcons().add(new Image("javafx/css/image/icon.png"));
        }
        return progressStage;
    }

    private Stage detailsEditorStage;

    public Stage getDetailsEditorStage() throws IOException {
        if (detailsEditorStage == null) {
            detailsEditorStage = new Stage();
            detailsEditorStage.setTitle("Конфигуратор");
            detailsEditorStage.setScene(new Scene(configurationControllers.getDetailsEditorView().getView()));
            detailsEditorStage.setResizable(false);
            detailsEditorStage.initOwner(applicationStage);
            detailsEditorStage.initModality(Modality.APPLICATION_MODAL);
            detailsEditorStage.centerOnScreen();
            detailsEditorStage.getIcons().add(new Image("javafx/css/image/icon.png"));
            detailsEditorStage.setOnHiding(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    try {
                        getPluginConfigurationStage().show();
                    } catch (IOException e) {
                        log.error("detailsEditorStage can't show!\n", e);
                    }
                }
            });
        }
        return detailsEditorStage;
    }

    private Stage mgeConfigurationStage;

    public Stage getMgeConfigurationStage() throws IOException {
        if (mgeConfigurationStage == null) {
            mgeConfigurationStage = new Stage();
            mgeConfigurationStage.setTitle("MGE");
            mgeConfigurationStage.setScene(new Scene(configurationControllers.getMGEConfigurationView().getView()));
            mgeConfigurationStage.setResizable(false);
            mgeConfigurationStage.initOwner(applicationStage);
            mgeConfigurationStage.getScene().getStylesheets().add("javafx/css/style.css");
            mgeConfigurationStage.initModality(Modality.APPLICATION_MODAL);
            mgeConfigurationStage.centerOnScreen();
            mgeConfigurationStage.getIcons().add(new Image("javafx/css/image/icon.png"));
        }
        return mgeConfigurationStage;
    }
}
