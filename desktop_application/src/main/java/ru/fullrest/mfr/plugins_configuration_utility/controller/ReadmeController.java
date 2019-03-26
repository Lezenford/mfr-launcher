package ru.fullrest.mfr.plugins_configuration_utility.controller;

import com.sun.javafx.webkit.Accessor;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.PluginsConfigurationUtilityApplication;
import ru.fullrest.mfr.plugins_configuration_utility.config.PropertiesConfiguration;
import ru.fullrest.mfr.plugins_configuration_utility.manager.StageManager;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@Log4j2
public class ReadmeController implements AbstractController {

    @Autowired
    private StageManager stageManager;

    @Autowired
    private PropertiesConfiguration propertiesConfiguration;

    @Autowired
    private PluginsConfigurationUtilityApplication application;

    @FXML
    private VBox mainPane;

    @FXML
    private Button minMaxButton;

    private URL url;

    @Override
    public void beforeOpen() {
        try {
            File file = new File(propertiesConfiguration.getGamePath() + "\\" + propertiesConfiguration.getReadme());
            url = file.toURI().toURL();
            WebView webView = new WebView();
            mainPane.getChildren().clear();
            mainPane.getChildren().add(webView);
            VBox.setVgrow(webView, Priority.ALWAYS);
            webView.getEngine().load(String.valueOf(url));
            Accessor.getPageFor(webView.getEngine()).setBackgroundColor(10);
            initButtonText();
        } catch (MalformedURLException e) {
            log.error("Can't create readme URI");
        }
    }

    public void close() {
        stageManager.getReadmeStage().close();
    }

    public void openInBrowser() {
        if (url != null) {
            application.getHostServices().showDocument(String.valueOf(url));
            close();
        }
    }

    public void changeSize() {
        stageManager.getReadmeStage().setMaximized(!stageManager.getReadmeStage().isMaximized());
        initButtonText();
    }

    private void initButtonText() {
        if (stageManager.getReadmeStage().isMaximized()) {
            minMaxButton.setText("Восстановить");
        } else {
            minMaxButton.setText("Развернуть");
        }
    }
}
