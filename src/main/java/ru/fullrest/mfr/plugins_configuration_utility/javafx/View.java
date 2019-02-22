package ru.fullrest.mfr.plugins_configuration_utility.javafx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import lombok.Data;
import ru.fullrest.mfr.plugins_configuration_utility.PluginsConfigurationUtilityApplication;
import ru.fullrest.mfr.plugins_configuration_utility.controller.AbstractController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Класс - оболочка: контроллер мы обязаны указать в качестве бина,
 * а view - представление, нам предстоит использовать в точке входа {@link PluginsConfigurationUtilityApplication}.
 */
@Data
public class View<T extends AbstractController> {

    private Parent view;
    private T controller;

    /**
     * Самый обыкновенный способ использовать FXML загрузчик.
     * На этом этапе будет создан объект-контроллер,
     * произведены все FXML инъекции и вызван метод инициализации контроллера.
     */
    public View(String url) throws IOException {
        try (InputStream fxmlStream = getClass().getClassLoader().getResourceAsStream(url)) {
            FXMLLoader loader = new FXMLLoader();
            loader.load(Objects.requireNonNull(fxmlStream));
            view = loader.getRoot();
            controller = loader.getController();
        }
    }
}
