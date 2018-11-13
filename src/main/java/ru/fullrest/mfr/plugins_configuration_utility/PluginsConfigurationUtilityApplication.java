package ru.fullrest.mfr.plugins_configuration_utility;

import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogEvent;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ResourceLoader;
import ru.fullrest.mfr.plugins_configuration_utility.config.ConfigurationControllers;
import ru.fullrest.mfr.plugins_configuration_utility.config.StageControllers;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.manager.RepositoryManager;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository;

import java.io.File;
import java.io.IOException;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
@Log4j2
@Lazy
@SpringBootApplication
public class PluginsConfigurationUtilityApplication extends AbstractJavaFxApplicationSupport {

    @Autowired
    private ConfigurationControllers configurationControllers;

    @Autowired
    private StageControllers stageControllers;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private PropertiesRepository repository;

    @Autowired
    private RepositoryManager repositoryManager;

    @Autowired
    private ResourceLoader loader;

    public static void main(String[] args) {
        launchApp(PluginsConfigurationUtilityApplication.class, args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stageControllers.setApplicationStage(primaryStage);
        configurationControllers.getMainView().getController().getGamePath().setText(repository.findByKey(PropertyKey.GAME_DIRECTORY_PATH).getValue());
        primaryStage.show();
        startCheckGameDirectory();
    }

    private void startCheckGameDirectory() {
        String value = repository.findByKey(PropertyKey.GAME_DIRECTORY_PATH).getValue();
        File folder;
        if (value == null) {
            folder = new File(new File("").getAbsolutePath());
            if (folder.exists() && folder.isDirectory()) {
                folder = new File(folder.getParent());
            }
        } else {
            folder = new File(value);
        }
        if (!fileManager.gameFolderCheck(folder)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ошибка!");
            alert.setHeaderText("Каталог с Morrowind не найден!");
            alert.setContentText("Необходимо указать путь до папки с игрой");
            alert.setOnCloseRequest(new EventHandler<DialogEvent>() {
                @Override
                public void handle(DialogEvent event) {
                    fileManager.initGameDirectory();
                }
            });
            alert.showAndWait();
        } else {
            fileManager.setGamePath(folder.getAbsolutePath());
            try {
                configurationControllers.getMainView().getController().getGamePath().setText(repository.findByKey(PropertyKey.GAME_DIRECTORY_PATH).getValue());
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileManager.checkVersion();
        }

        value = repository.findByKey(PropertyKey.DEFAULT_DETAILS_INIT).getValue();
        if (value == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Внимание!");
            alert.setHeaderText(null);
            alert.setContentText("При первом запуске программы необходимо проинициализировать скрипты.\n" + "Это " +
                    "может занять некоторое время, подождите, пожалуйста");
            alert.setOnCloseRequest(new EventHandler<DialogEvent>() {
                @Override
                public void handle(DialogEvent event) {
                    try {
                        File temp = new File("");
                        File file =
                                new File(temp.getAbsolutePath() + FileManager.SEPARATOR + "script" + FileManager.SEPARATOR + "script.sql");
                        if (file.exists()) {
                            configurationControllers.getProgressView().getController().beforeOpen();
                            configurationControllers.getProgressView().getController().execSqlScript("script" + FileManager.SEPARATOR + "script.sql", false, true);
                            stageControllers.getProgressStage().show();
                        } else {
                            configurationControllers.getProgressView().getController().beforeOpen();
                            configurationControllers.getProgressView().getController().execSqlScript("script.sql",
                                    true, true);
                            stageControllers.getProgressStage().show();
                        }
                    } catch (IOException e) {
                        log.error(e.getMessage());
                    }
                }
            });
            alert.showAndWait();
        }
    }
}
