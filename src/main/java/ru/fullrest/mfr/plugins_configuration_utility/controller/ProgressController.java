package ru.fullrest.mfr.plugins_configuration_utility.controller;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.manager.RepositoryManager;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.*;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.DetailsRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.GroupRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.ReleaseRepository;

import javax.persistence.PersistenceException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
public class ProgressController extends AbstractController {

    @Autowired
    private ResourceLoader loader;

    @Autowired
    private RepositoryManager repositoryManager;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private DetailsRepository detailsRepository;

    @Autowired
    private PropertiesRepository propertiesRepository;

    @Autowired
    private FileManager fileManager;

    @FXML
    private ProgressIndicator progressBar;

    @FXML
    private Label informationLabel;

    @FXML
    private Label headerLabel;

    @FXML
    private Button closeButton;

    @Override
    public void beforeOpen() {
        closeButton.setDisable(true);
    }

    public void acceptPluginChanges(Map<Group, ToggleGroup> groupMap) {
        FileManagerTask task = new FileManagerTask();
        task.setGroupMap(groupMap);
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        informationLabel.textProperty().unbind();
        informationLabel.textProperty().bind(task.messageProperty());
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                closeButton.setDisable(false);
                if (task.getValue()) {
                    headerLabel.setText("Загрузка завершена");
                } else {
                    headerLabel.setText("Ошибка загрузки!");
                }
            }
        });
        new Thread(task).start();
    }

    public void execSqlScript(String path, boolean classpath, boolean initScript) {
        ExecuteSQLTask task = new ExecuteSQLTask();
        task.setPath(path);
        task.setClasspath(classpath);
        task.setInitScript(initScript);
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        informationLabel.textProperty().unbind();
        informationLabel.textProperty().bind(task.messageProperty());
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                closeButton.setDisable(false);
                if (task.getValue()) {
                    headerLabel.setText("Загрузка завершена");
                    if (initScript) {
                        closeButton.setDisable(true);
                        Properties properties = propertiesRepository.findByKey(PropertyKey.DEFAULT_DETAILS_INIT);
                        properties.setValue("");
                        propertiesRepository.save(properties);
                        calculateMD5(initScript);
                    }
                } else {
                    headerLabel.setText("Ошибка загрузки!");
                }
            }
        });
        new Thread(task).start();
    }

    public void calculateMD5(boolean initScript) {
        CalculateMD5Task task = new CalculateMD5Task();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        informationLabel.textProperty().unbind();
        informationLabel.textProperty().bind(task.messageProperty());
        headerLabel.setText("Анализ опциональных файлов");
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                closeButton.setDisable(false);
                headerLabel.setText("Анализ файлов завершен");
                if (initScript) {
                    closeButton.setDisable(true);
                    findActivePluginByMD5();
                }
            }
        });
        new Thread(task).start();
    }

    public void findActivePluginByMD5() {
        FindActivePluginByMD5Task task = new FindActivePluginByMD5Task();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        informationLabel.textProperty().unbind();
        informationLabel.textProperty().bind(task.messageProperty());
        headerLabel.setText("Поиск активированных плагинов");
        task.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                headerLabel.setText("Поиск завершен");
                closeButton.setDisable(false);
            }
        });
        new Thread(task).start();
    }

    public void closeButtonAction() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

    private class ExecuteSQLTask extends Task<Boolean> {

        @Setter
        private String path;

        @Setter
        private boolean classpath;

        @Setter
        private boolean initScript;

        @Override
        protected Boolean call() {
            File script;
            int maxCount = 0;
            this.updateProgress(0, maxCount);
            this.updateMessage("Считывание файла скрипта");
            if (classpath) {
                try {
                    script = Files.createTempFile("tmp_", ".sql").toFile();
                    script.deleteOnExit();
                    try (InputStream in = loader.getResource("classpath:" + path).getInputStream(); OutputStream out
                            = new FileOutputStream(script)) {
                        IOUtils.copy(in, out);
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                    this.updateMessage(e.getMessage());
                    return false;
                }
            } else {
                script = new File(path);
            }
            if (script.exists() && script.isFile() && script.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(script))) {
                    while (reader.ready()) {
                        if (reader.readLine().contains(";")) {
                            maxCount++;
                        }
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                    this.updateMessage(e.getMessage());
                    return false;
                }
                try (BufferedReader reader = new BufferedReader(new FileReader(script, Charset.forName("UTF-8")))) {
                    StringBuilder builder = new StringBuilder();
                    int currentCount = 0;
                    while (reader.ready()) {
                        String temp = reader.readLine();
                        if (temp.contains(";")) {
                            builder.append(temp);
                            currentCount++;
                            if (currentCount <= maxCount) {
                                this.updateProgress(currentCount, maxCount);
                            }
                            this.updateMessage("Выполняется команда: " + builder.toString().strip());
                            try {
                                repositoryManager.initUpdateScript(builder.toString());
                            } catch (PersistenceException e) {
                                log.error(e.getMessage());
                            }
                            builder = new StringBuilder();
                        } else {
                            builder.append(temp).append(" ");
                        }
                    }
                } catch (IOException e) {
                    log.error(e.getMessage());
                    this.updateMessage(e.getMessage());
                    return false;
                }
                this.updateProgress(1, 1);
                this.updateMessage("");
                if (classpath) {
                    if (!script.delete()) {
                        log.error("Temp script file has not deleted! " + script.getAbsolutePath());
                    }
                }
                return true;
            } else {
                this.updateProgress(0, 0);
                this.updateMessage("Не удалось прочитать файл скрипта!");
                return false;
            }
        }
    }

    private class FileManagerTask extends Task<Boolean> {

        @Setter
        private Map<Group, ToggleGroup> groupMap;

        @Override
        protected Boolean call() throws Exception {
            for (Map.Entry<Group, ToggleGroup> entry : groupMap.entrySet()) {
                List<Release> releases =
                        releaseRepository.findAllByGroupAndAppliedIsTrueAndActiveIsTrue(entry.getKey());
                this.updateMessage("Проверка отключаемых модулей");
                if (entry.getValue().getSelectedToggle() != null && entry.getValue().getSelectedToggle().getUserData() instanceof Release && !releases.contains((Release) entry.getValue().getSelectedToggle().getUserData())) {
                    for (Release release : releases) {
                        this.updateMessage("Отключение модуля: " + release.getValue());
                        this.updateProgress(0, 0);
                        List<Details> details = detailsRepository.findAllByReleaseAndActiveIsTrue(release);
                        for (int i = 0; i < details.size(); i++) {
                            this.updateProgress(i, details.size());
                            if (fileManager.checkFileMD5(details.get(i))) {
                                fileManager.removeFromGameDirectory(details.get(i));
                            }
                        }
                        release.setApplied(false);
                        releaseRepository.save(release);
                    }
                    Release release = (Release) entry.getValue().getSelectedToggle().getUserData();
                    List<Details> detailsList = detailsRepository.findAllByReleaseAndActiveIsTrue(release);
                    int count = 0;
                    for (Details details : detailsList) {
                        count++;
                        this.updateMessage("Подключение модуля: " + details.getRelease().getValue());
                        this.updateProgress(count, detailsList.size());
                        fileManager.copyToGameDirectory(details);
                    }
                    this.updateMessage("Сохранение изменений");
                    this.updateProgress(1, 1);
                    release.setApplied(true);
                    releaseRepository.save(release);
                }
                this.updateProgress(1, 1);
                this.updateMessage("");
            }
            return true;
        }
    }

    private class CalculateMD5Task extends Task<List<Details>> {

        @Override
        protected List<Details> call() throws Exception {
            List<Details> resultList = new ArrayList<>();
            List<Details> detailsList = detailsRepository.findAllByActiveIsTrue();
            for (int i = 0; i < detailsList.size(); i++) {
                File file = new File(fileManager.getOptionalPath(true) + detailsList.get(i).getStoragePath());
                this.updateMessage("Расчет метаинформации для файла: " + file.getAbsolutePath());
                this.updateProgress(i, detailsList.size());
                if (file.exists()) {
                    try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
                        MessageDigest digest = MessageDigest.getInstance("MD5");
                        digest.update(bufferedInputStream.readAllBytes());
                        detailsList.get(i).setMd5(digest.digest());
                        detailsRepository.save(detailsList.get(i));
                        this.updateMessage("Расчет успешно завершен");
                        this.updateProgress(i, detailsList.size());
                    }
                } else {
                    resultList.add(detailsList.get(i));
                    this.updateMessage("Ошибка расчета MD5");
                    this.updateProgress(i, detailsList.size());
                    log.error("File doesn't exist!\n" + file.getAbsolutePath());
                }
            }
            this.updateMessage("Расчет успешно завершен");
            this.updateProgress(1, 1);
            return resultList;
        }
    }

    private class FindActivePluginByMD5Task extends Task<Boolean> {

        @Override
        protected Boolean call() throws Exception {
            for (Group group : groupRepository.findAllByActiveIsTrue()) {
                this.updateMessage("Поиск активной конфигурации для " + group.getValue());
                boolean foundRelease = false;
                for (Release release : releaseRepository.findAllByGroupAndActiveIsTrue(group)) {
                    this.updateProgress(0, 0);
                    release.setApplied(false);
                    if (!foundRelease) {
                        List<Details> details = detailsRepository.findAllByReleaseAndActiveIsTrue(release);
                        for (int i = 0; i < details.size(); i++) {
                            updateProgress(i, details.size());
                            if (!fileManager.checkFileMD5(details.get(i))) {
                                break;
                            }
                            if (i == details.size() - 1) {
                                foundRelease = true;
                                release.setApplied(true);
                            }
                        }
                    }
                    releaseRepository.save(release);
                }
            }
            this.updateProgress(1, 1);
            this.updateMessage("");
            return null;
        }
    }
}