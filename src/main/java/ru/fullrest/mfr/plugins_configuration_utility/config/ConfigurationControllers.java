package ru.fullrest.mfr.plugins_configuration_utility.config;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.fullrest.mfr.plugins_configuration_utility.PluginsConfigurationUtilityApplication;
import ru.fullrest.mfr.plugins_configuration_utility.controller.*;

import javax.persistence.EnumType;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;

/**
 * Created on 01.11.2018
 *
 * @author Alexey Plekhanov
 */
@Configuration
public class ConfigurationControllers {

    @SuppressWarnings("unchecked")
    @Bean(name = "mainView")
    public View<MainController> getMainView() throws IOException {
        return (View<MainController>) loadView("javafx/main.fxml");
    }

    @SuppressWarnings("unchecked")
    @Bean(name = "configurationView")
    public View<PluginConfigurationController> getConfigurationView() throws IOException {
        return (View<PluginConfigurationController>) loadView("javafx/plugin_configuration.fxml");
    }

    @SuppressWarnings("unchecked")
    @Bean(name = "progressView")
    public View<ProgressController> getProgressView() throws IOException {
        return (View<ProgressController>) loadView("javafx/progress.fxml");
    }

    @SuppressWarnings("unchecked")
    @Bean(name = "detailsEditorView")
    public View<DetailsEditorController> getDetailsEditorView() throws IOException {
        return (View<DetailsEditorController>) loadView("javafx/details-editor.fxml");
    }


    @SuppressWarnings("unchecked")
    @Bean(name = "mgeConfigurationView")
    public View<MGEConfigurationController> getMGEConfigurationView() throws IOException {
        return (View<MGEConfigurationController>)  loadView("javafx/mge-configuration.fxml");
    }

    /**
     * Именно благодаря этому методу мы добавили контроллер в контекст спринга,
     * и заставили его произвести все необходимые инъекции.
     */
    @Bean
    public MainController getMainController() throws IOException {
        return getMainView().getController();
    }

    @Bean
    public PluginConfigurationController getPluginConfigurationController() throws IOException {
        return getConfigurationView().getController();
    }

    @Bean
    public ProgressController getProgressController() throws IOException {
        return getProgressView().getController();
    }

    @Bean
    public DetailsEditorController getDetailsEditorsController() throws IOException {
        return getDetailsEditorView().getController();
    }

    @Bean
    public MGEConfigurationController getMGEConfigurationController() throws IOException {
        return getMGEConfigurationView().getController();
    }

    /**
     * Самый обыкновенный способ использовать FXML загрузчик.
     * Как раз-таки на этом этапе будет создан объект-контроллер,
     * произведены все FXML инъекции и вызван метод инициализации контроллера.
     */
    private View<?> loadView(String url) throws IOException {
        try (InputStream fxmlStream = getClass().getClassLoader().getResourceAsStream(url)) {
            FXMLLoader loader = new FXMLLoader();
            loader.load(fxmlStream);
            return new View<>(loader.getRoot(), loader.getController());
        }
    }

    /**
     * Класс - оболочка: контроллер мы обязаны указать в качестве бина,
     * а view - представление, нам предстоит использовать в точке входа {@link PluginsConfigurationUtilityApplication}.
     */
    @Data
    @AllArgsConstructor
    public class View<T extends AbstractController> {
        private Parent view;
        private T controller;
    }
}
