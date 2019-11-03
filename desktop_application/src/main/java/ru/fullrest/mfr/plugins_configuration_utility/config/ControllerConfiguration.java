package ru.fullrest.mfr.plugins_configuration_utility.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.fullrest.mfr.plugins_configuration_utility.controller.*;
import ru.fullrest.mfr.plugins_configuration_utility.controller.details_editor.AddBaseEntityController;
import ru.fullrest.mfr.plugins_configuration_utility.controller.details_editor.AddDetailsController;
import ru.fullrest.mfr.plugins_configuration_utility.controller.details_editor.DetailsEditorController;
import ru.fullrest.mfr.plugins_configuration_utility.javafx.View;

import java.io.IOException;

/**
 * Created on 01.11.2018
 *
 * @author Alexey Plekhanov
 */

@Configuration
public class ControllerConfiguration {

    /**
     * С помощью этих методов мы добавиляем view в контекст спринга,
     */
    @Bean
    public View<MainController> mainView() throws IOException {
        return new View<>("javafx/main.fxml");
    }

    @Bean
    public View<PluginConfigurationController> configurationView() throws IOException {
        return new View<>("javafx/plugin-configuration.fxml");
    }

    @Bean
    public View<ProgressController> progressView() throws IOException {
        return new View<>("javafx/progress.fxml");
    }

    @Bean
    public View<DetailsEditorController> detailsEditorView() throws IOException {
        return new View<>("javafx/details-editor.fxml");
    }

    @Bean
    public View<MGEConfigurationController> mgeConfigurationView() throws IOException {
        return new View<>("javafx/mge-configuration.fxml");
    }

    @Bean
    public View<ReadmeController> readmeView() throws IOException {
        return new View<>("javafx/readme.fxml");
    }

    @Bean
    public View<AddDetailsController> addDetailsView() throws IOException {
        return new View<>("javafx/add-details.fxml");
    }

    @Bean
    public View<AddBaseEntityController> addBaseEntityView() throws IOException {
        return new View<>("javafx/add-base-entity.fxml");
    }

    @Bean
    public View<HelpForProjectController> helpForProjectView() throws IOException {
        return new View<>("javafx/help-for-project.fxml");
    }

    @Bean
    public View<AlertController> alertView() throws IOException {
        return new View<>("javafx/alert.fxml");
    }

    @Bean
    public View<AlertNewsController> alertNewsView() throws IOException {
        return new View<>("javafx/alert_news.fxml");
    }

    /**
     * С помощью этих методов мы добавиляем контроллеры в контекст спринга,
     */
    @Bean
    public MainController mainController() throws IOException {
        return mainView().getController();
    }

    @Bean
    public PluginConfigurationController pluginConfigurationController() throws IOException {
        return configurationView().getController();
    }

    @Bean
    public ProgressController progressController() throws IOException {
        return progressView().getController();
    }

    @Bean(initMethod = "init")
    public DetailsEditorController detailsEditorsController() throws IOException {
        return detailsEditorView().getController();
    }

    @Bean(initMethod = "init")
    public MGEConfigurationController mgeConfigurationController() throws IOException {
        return mgeConfigurationView().getController();
    }

    @Bean
    public ReadmeController readmeController() throws IOException {
        return readmeView().getController();
    }

    @Bean(initMethod = "init")
    public AddDetailsController addDetailsController() throws IOException {
        return addDetailsView().getController();
    }

    @Bean(initMethod = "init")
    public AddBaseEntityController addBaseEntityController() throws IOException {
        return addBaseEntityView().getController();
    }

    @Bean
    public HelpForProjectController helpForProjectController() throws IOException {
        return helpForProjectView().getController();
    }

    @Bean
    public AlertController alertController() throws IOException {
        return alertView().getController();
    }

    @Bean
    public AlertNewsController alertNewsController() throws IOException {
        return alertNewsView().getController();
    }
}