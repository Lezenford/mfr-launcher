package ru.fullrest.mfr.plugins_configuration_utility.javafx;

import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.fullrest.mfr.plugins_configuration_utility.config.PropertiesConfiguration;
import ru.fullrest.mfr.plugins_configuration_utility.controller.HelpForProjectController;
import ru.fullrest.mfr.plugins_configuration_utility.controller.MainController;
import ru.fullrest.mfr.plugins_configuration_utility.controller.ProgressController;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.manager.StageManager;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Properties;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.GroupRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

@Log4j2
@Component
public class StartApplicationInstruction {
    @Autowired
    private View<MainController> mainView;

    @Autowired
    private View<ProgressController> progressView;

    @Autowired
    private View<HelpForProjectController> helpForProjectView;

    @Autowired
    private StageManager stageManager;

    @Autowired
    private FileManager fileManager;

    @Autowired
    private PropertiesRepository propertiesRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PropertiesConfiguration propertiesConfiguration;

    public void startApplication(Stage primaryStage, String[] args) {
        log.debug("Start application init");
        checkGamePathProperty();
        checkVersion();
        stageManager.setApplicationStage(primaryStage);
        stageManager.getApplicationStage().show();
        checkUpdateDB();
        checkExtendedMod(args);
        checkFirstStart();
        log.debug("Start application instructions completed");
    }

    private void checkGamePathProperty() {
        log.debug("Method checkGamePathProperty started");

        File gameFolder = new File(new File("").getAbsolutePath());
        if (gameFolder.getParent() != null) {
            gameFolder = new File(gameFolder.getParent());
        }
        log.debug(String.format("Game path: %s", gameFolder.getAbsolutePath()));
        if (fileManager.gameFolderCheck(gameFolder)) {
            log.debug("Game directory is correct");
            propertiesConfiguration.setGamePath(gameFolder.getAbsolutePath());
        } else {
            log.error("Game directory not found!");
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка!");
            alert.setHeaderText("Каталог с Morrowind не найден!");
            alert.setOnCloseRequest(event -> System.exit(0));
            alert.showAndWait();
        }

        //Заполняем окно информацией
        log.debug("Set gamePath to view");
        mainView.getController().getGamePath().setText(propertiesConfiguration.getGamePath());//TODO убрать после редизайна;

        log.debug("Method checkGamePathProperty finished");
    }

    private void checkVersion() {
        log.debug("Method checkVersion started");

        String version = fileManager.checkVersion();
        propertiesConfiguration.setVersion(Objects.requireNonNullElse(version, ""));
        mainView.getController().getVersion().setText(propertiesConfiguration.getVersion());

        log.debug("Method checkVersion finished");
    }

    private void checkUpdateDB() {
        log.debug("Method checkUpdateDB started");

        Properties versionProperty = propertiesRepository.findByKey(PropertyKey.VERSION);
        if (versionProperty == null || !versionProperty.getValue().equals(propertiesConfiguration.getVersion())) {
            log.debug("Schema need to be updated");
            File schema = fileManager.getSchemaFile();
            Properties schemaProperty = propertiesRepository.findByKey(PropertyKey.SCHEMA);
            if (schemaProperty == null) {
                schemaProperty = new Properties();
                schemaProperty.setKey(PropertyKey.SCHEMA);
                schemaProperty.setValue("");
            }
            if (schema != null) {
                log.debug("Schema file exists");
                byte[] schemaMD5 = fileManager.getFileMD5(schema);
                if (schemaMD5 != null) {
                    if (!Arrays.toString(schemaMD5).equals(schemaProperty.getValue())) {
                        log.debug("Current schema is different from the new one");
                        groupRepository.deleteAll();
                        progressView.getController().beforeOpen();
                        progressView.getController().readJson(schema);
                        stageManager.getProgressStage().showAndWait();
                        if (versionProperty == null) {
                            versionProperty = new Properties();
                            versionProperty.setKey(PropertyKey.VERSION);
                        }
                        versionProperty.setValue(propertiesConfiguration.getVersion());
                        propertiesRepository.save(versionProperty);
                        log.debug("Version has saved");
                        schemaProperty.setValue(Arrays.toString(schemaMD5));
                        propertiesRepository.save(schemaProperty);
                        log.debug("Schema has saved");
                    }
                }
            }
        }

        log.debug("Method checkUpdateDB finished");
    }

    private void checkExtendedMod(String[] args) {
        log.debug("Method checkExtendedMod started");
        for (String arg : args) {
            if (arg.equals("-extend")) {
                propertiesConfiguration.setExtendedMod(true);
                log.debug("Method checkExtendedMod finished");
                return;
            }
        }
        propertiesConfiguration.setExtendedMod(false);
        log.debug("Method checkExtendedMod finished");
    }

    private void checkFirstStart() {
        Properties firstStart = propertiesRepository.findByKey(PropertyKey.FIRST_START);
        if (firstStart == null) {
            helpForProjectView.getController().setFirstStart(true);
            helpForProjectView.getController().beforeOpen();
            stageManager.getHelpForProjectStage().show();
            firstStart = new Properties();
            firstStart.setKey(PropertyKey.FIRST_START);
            propertiesRepository.save(firstStart);
        }
    }
}
