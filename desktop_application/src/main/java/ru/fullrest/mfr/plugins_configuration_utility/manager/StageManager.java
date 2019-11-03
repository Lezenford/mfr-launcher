package ru.fullrest.mfr.plugins_configuration_utility.manager;

import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import ru.fullrest.mfr.plugins_configuration_utility.controller.*;
import ru.fullrest.mfr.plugins_configuration_utility.controller.details_editor.AddBaseEntityController;
import ru.fullrest.mfr.plugins_configuration_utility.controller.details_editor.AddDetailsController;
import ru.fullrest.mfr.plugins_configuration_utility.controller.details_editor.DetailsEditorController;
import ru.fullrest.mfr.plugins_configuration_utility.javafx.View;


@Log4j2
@Component
@RequiredArgsConstructor
public class StageManager {

    private final View<MainController> mainView;
    @Getter
    private Stage applicationStage;

    private final View<PluginConfigurationController> pluginConfigurationView;
    private Stage pluginConfigurationStage;

    private final View<ProgressController> progressView;
    private Stage progressStage;

    private final View<DetailsEditorController> detailsEditorView;
    private Stage detailsEditorStage;

    private final View<MGEConfigurationController> mgeConfigurationView;
    private Stage mgeConfigurationStage;

    private final View<ReadmeController> readmeView;
    private Stage readmeStage;

    private final View<AddDetailsController> detailsView;
    private Stage addDetailsStage;

    private final View<AddBaseEntityController> addBaseEntityView;
    private Stage addBaseEntityStage;

    private final View<HelpForProjectController> helpForProjectView;
    private Stage helpForProjectStage;

    private final View<AlertController> alertView;
    private Stage alertStage;

    private final View<AlertNewsController> alertNewsView;
    private Stage alertNewsStage;

    public void setApplicationStage(Stage applicationStage) {
        this.applicationStage = applicationStage;
        initStage(applicationStage, "M[FR] Настройки репака", mainView);
    }

    public Stage getPluginConfigurationStage() {
        if (pluginConfigurationStage == null) {
            pluginConfigurationStage = initStage("Конфигурация", pluginConfigurationView);
            pluginConfigurationStage.setOnHiding(event -> getApplicationStage().show());
        }
        return pluginConfigurationStage;
    }

    public Stage getProgressStage() {
        if (progressStage == null) {
            progressStage = initStage("Пожалуйста, подождите...", progressView);
            progressStage.setOnCloseRequest(Event::consume); //This prevents user from closing the window
        }
        return progressStage;
    }

    public Stage getDetailsEditorStage() {
        if (detailsEditorStage == null) {
            detailsEditorStage = initStage("Конфигуратор", detailsEditorView);
            detailsEditorStage.setOnHiding(event -> {
                pluginConfigurationView.getController().beforeOpen();
                getPluginConfigurationStage().show();
            });
        }
        return detailsEditorStage;
    }

    public Stage getMgeConfigurationStage() {
        if (mgeConfigurationStage == null) {
            mgeConfigurationStage = initStage("MGE", mgeConfigurationView);
        }
        return mgeConfigurationStage;
    }

    public Stage getReadmeStage() {
        if (readmeStage == null) {
            readmeStage = initStage("Readme", readmeView);
            readmeStage.setResizable(true);
            readmeStage.setOnHidden(event -> getApplicationStage().show());
        }
        return readmeStage;
    }

    public Stage getAddDetailsStage() {
        if (addDetailsStage == null) {
            addDetailsStage = initStage("Доблавить файлы", detailsView);
        }
        return addDetailsStage;
    }

    public Stage getAddBaseEntityStage() {
        if (addBaseEntityStage == null) {
            addBaseEntityStage = initStage("Новая сущность", addBaseEntityView);
        }
        return addBaseEntityStage;
    }

    public Stage getHelpForProjectStage() {
        if (helpForProjectStage == null) {
            helpForProjectStage = initStage("Помочь проекту", helpForProjectView);
        }
        return helpForProjectStage;
    }

    public Stage getAlertStage() {
        if (alertStage == null) {
            alertStage = initStage("Помочь проекту", alertView);
        }
        return alertStage;
    }

    public Stage getAlertNewsStage() {
        if (alertNewsStage == null) {
            alertNewsStage = initStage("Помочь проекту", alertNewsView);
        }
        return alertNewsStage;
    }

    private Stage initStage(String title, View view) {
        Stage stage = new Stage();
        initStage(stage, title, view);
        return stage;
    }

    private void initStage(Stage stage, String title, View view) {
        stage.setResizable(false);
        if (applicationStage != null && !applicationStage.equals(stage)) { //Этот шаг не нужен для applicationStage
            stage.initOwner(applicationStage);
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setScene(new Scene(view.getView(), Color.TRANSPARENT));
        stage.getScene().getStylesheets().add("javafx/css/style.css");
        stage.centerOnScreen();
        stage.getIcons().add(new Image("javafx/css/image/icon.png"));
        stage.setTitle(title);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (!stage.equals(applicationStage) && event.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
    }
}