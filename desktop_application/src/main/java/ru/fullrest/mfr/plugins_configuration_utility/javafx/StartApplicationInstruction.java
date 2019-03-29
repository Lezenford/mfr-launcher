package ru.fullrest.mfr.plugins_configuration_utility.javafx;

import javafx.scene.control.Alert;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
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
@RequiredArgsConstructor
public class StartApplicationInstruction {
    private final View<MainController> mainView;
    private final View<ProgressController> progressView;
    private final View<HelpForProjectController> helpForProjectView;
    private final StageManager stageManager;
    private final FileManager fileManager;
    private final PropertiesRepository propertiesRepository;
    private final GroupRepository groupRepository;
    private final PropertiesConfiguration propertiesConfiguration;

    public void startApplication(Stage primaryStage, String[] args) {
        log.info("Start application init");
        checkGamePathProperty();
        checkVersion();
        stageManager.setApplicationStage(primaryStage);
        stageManager.getApplicationStage().show();
        checkUpdateDB();
        checkExtendedMod(args);
        checkFirstStart();
        log.info("Start application instructions completed");
    }

    private void checkGamePathProperty() {
        log.info("Method checkGamePathProperty started");

        File gameFolder = new File(new File("").getAbsolutePath());
        if (gameFolder.getParent() != null) {
            gameFolder = new File(gameFolder.getParent());
        }
        log.info(String.format("Game path: %s", gameFolder.getAbsolutePath()));
        if (fileManager.gameFolderCheck(gameFolder)) {
            log.info("Game directory is correct");
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
        log.info("Set gamePath to view");
        mainView.getController().getGamePath()
                .setText(propertiesConfiguration.getGamePath());//TODO убрать после редизайна;

        log.info("Method checkGamePathProperty finished");
    }

    private void checkVersion() {
        log.info("Method checkVersion started");

        progressView.getController().beforeOpen();
        progressView.getController().checkApplicationUpdate();
        stageManager.getProgressStage().showAndWait();
        String version = fileManager.checkVersion();
        progressView.getController().beforeOpen();
        progressView.getController().checkGameUpdate(version, true);
        stageManager.getProgressStage().showAndWait();
        version = fileManager.checkVersion();
        propertiesConfiguration.setGameVersion(Objects.requireNonNullElse(version, ""));
        mainView.getController().getVersion().setText(propertiesConfiguration.getGameVersion());

        log.info("Method checkVersion finished");
    }

    private void checkUpdateDB() {
        log.info("Method checkUpdateDB started");

        Properties versionProperty = propertiesRepository.findByKey(PropertyKey.VERSION);
        if (versionProperty == null || !versionProperty.getValue().equals(propertiesConfiguration
                .getGameVersion()) || propertiesConfiguration.isRefreshSchema()) {
            log.info("Schema need to be updated");
            File schema = fileManager.getSchemaFile();
            Properties schemaProperty = propertiesRepository.findByKey(PropertyKey.SCHEMA);
            if (schemaProperty == null) {
                schemaProperty = new Properties();
                schemaProperty.setKey(PropertyKey.SCHEMA);
                schemaProperty.setValue("");
            }
            if (schema != null) {
                log.info("Schema file exists");
                byte[] schemaMD5 = fileManager.getFileMD5(schema);
                if (schemaMD5 != null) {
                    if (!Arrays.toString(schemaMD5).equals(schemaProperty.getValue())) {
                        log.info("Current schema is different from the new one");
                        groupRepository.deleteAll();
                        progressView.getController().beforeOpen();
                        progressView.getController().readJson(schema);
                        stageManager.getProgressStage().showAndWait();
                        if (versionProperty == null) {
                            versionProperty = new Properties();
                            versionProperty.setKey(PropertyKey.VERSION);
                        }
                        versionProperty.setValue(propertiesConfiguration.getGameVersion());
                        propertiesRepository.save(versionProperty);
                        log.info("Version has saved");
                        schemaProperty.setValue(Arrays.toString(schemaMD5));
                        propertiesRepository.save(schemaProperty);
                        log.info("Schema has saved");
                    }
                }
            }
        }

        log.info("Method checkUpdateDB finished");
    }

    private void checkExtendedMod(String[] args) {
        log.info("Method checkExtendedMod started");
        for (String arg : args) {
            if (arg.equals("-extend")) {
                propertiesConfiguration.setExtendedMod(true);
                log.info("Method checkExtendedMod finished");
                return;
            }
        }
        propertiesConfiguration.setExtendedMod(false);
        log.info("Method checkExtendedMod finished");
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