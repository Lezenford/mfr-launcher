package ru.fullrest.mfr.plugins_configuration_utility.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Details;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group;
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.DetailsRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.GroupRepository;
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.ReleaseRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
public class ProgressController implements AbstractController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private ReleaseRepository releaseRepository;

    @Autowired
    private DetailsRepository detailsRepository;

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
        headerLabel.setText("Применение изменений");
        closeButton.setVisible(false);
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            closeButton.setDisable(false);
            closeButton.setVisible(true);
            if (task.getValue()) {
                headerLabel.setText("Загрузка завершена");
            } else {
                headerLabel.setText("Ошибка загрузки!");
            }
        });
        new Thread(task).start();
    }

    public void readJson(File file) {
        ReadJsonTask task = new ReadJsonTask();
        task.setFile(file);
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        informationLabel.textProperty().unbind();
        informationLabel.textProperty().bind(task.messageProperty());
        closeButton.setVisible(false);
        headerLabel.setText("Анализ конфигурации");
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            if (task.getValue()) {
                headerLabel.setText("Загрузка завершена");
            } else {
                headerLabel.setText("Ошибка загрузки");
            }
            calculateMD5();
        });
        new Thread(task).start();
    }

    public void calculateMD5() {
        CalculateMD5Task task = new CalculateMD5Task();
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().bind(task.progressProperty());
        informationLabel.textProperty().unbind();
        informationLabel.textProperty().bind(task.messageProperty());
        headerLabel.setText("Анализ опциональных файлов");
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            headerLabel.setText("Анализ файлов завершен");
            findActivePluginByMD5();
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
        task.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            headerLabel.setText("Поиск активной конфигурации завершен");
            closeButton.setVisible(true);
            closeButton.setDisable(false);
        });
        new Thread(task).start();
    }

    public void closeButtonAction() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

    private void checkFutures(List<Future> futures) {
        if (futures != null) {
            for (Future future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Future get error\n", e);
                }
            }
        }
    }

    private class ReadJsonTask extends Task<Boolean> {

        @Setter
        private File file;

        @Override
        protected Boolean call() {
            ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
            this.updateProgress(0, 0);
            this.updateMessage("Считывание данных из файла");
            List<String> jsonList = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file, Charset.forName("UTF-8")))) {
                while (reader.ready()) {
                    jsonList.add(reader.readLine());
                }
            } catch (IOException e) {
                log.error("Error read json file.\n", e);
            }
            this.updateMessage("Считывание данных из файла");
            List<Group> groups = new CopyOnWriteArrayList<>();
            List<Future> futures = new ArrayList<>();
            AtomicInteger count = new AtomicInteger();
            for (String json : jsonList) {
                futures.add(threadPool.submit(() -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Group group = mapper.readValue(json, Group.class);
                        group.getReleases().forEach(release -> {
                            release.setGroup(group);
                            release.getDetails().forEach(details -> details.setRelease(release));
                        });
                        groups.add(group);
                        this.updateProgress(count.incrementAndGet(), jsonList.size());
                    } catch (IOException e) {
                        log.error("Error mapping json to entity.\n", e);
                    }
                }));
            }
            checkFutures(futures);
            this.updateMessage("Сохранение данных");
            this.updateProgress(1, 1);
            groupRepository.saveAll(groups);
            this.updateMessage("Процесс завершен");
            threadPool.shutdownNow();
            return true;
        }
    }

    private class FileManagerTask extends Task<Boolean> {

        @Setter
        private Map<Group, ToggleGroup> groupMap;

        @Override
        protected Boolean call() {
            ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
            for (Map.Entry<Group, ToggleGroup> entry : groupMap.entrySet()) {
                List<Release> releases =
                        releaseRepository.findAllByGroupAndAppliedIsTrue(entry.getKey());
                List<Future> futures = new ArrayList<>();
                this.updateMessage("Проверка отключаемых модулей");
                if (entry.getValue().getSelectedToggle() != null && entry.getValue().getSelectedToggle().getUserData() instanceof Release && !releases.contains((Release) entry.getValue().getSelectedToggle().getUserData())) {
                    for (Release release : releases) {
                        this.updateMessage("Отключение модуля: " + release.getValue());
                        this.updateProgress(0, 0);
                        AtomicInteger count = new AtomicInteger();
                        List<Details> detailsForInactive = detailsRepository.findAllByRelease(release);
                        for (Details detail : detailsForInactive) {
                            futures.add(threadPool.submit(() -> {
                                if (fileManager.checkFileMD5(detail)) {
                                    fileManager.removeFromGameDirectory(detail);
                                }
                                this.updateProgress(count.incrementAndGet(), detailsForInactive.size());
                            }));
                        }
                        checkFutures(futures);
                        release.setApplied(false);
                        releaseRepository.save(release);
                    }
                    Release release = (Release) entry.getValue().getSelectedToggle().getUserData();
                    List<Details> detailsForActive = detailsRepository.findAllByRelease(release);
                    futures = new ArrayList<>();
                    AtomicInteger count = new AtomicInteger();
                    this.updateMessage("Подключение модуля: " + release.getValue());
                    for (Details details : detailsForActive) {
                        futures.add(threadPool.submit(() -> {
                            fileManager.copyToGameDirectory(details);
                            this.updateProgress(count.incrementAndGet(), detailsForActive.size());
                        }));
                    }
                    checkFutures(futures);
                    this.updateMessage("Сохранение изменений");
                    this.updateProgress(1, 1);
                    release.setApplied(true);
                    releaseRepository.save(release);
                }
                this.updateProgress(1, 1);
                this.updateMessage("");
            }
            threadPool.shutdownNow();
            return true;
        }
    }

    private class CalculateMD5Task extends Task<Boolean> {

        @Override
        protected Boolean call() {
            ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
            List<Future> futures = new ArrayList<>();
            AtomicInteger count = new AtomicInteger();
            List<Details> detailsList = new ArrayList<>();
            detailsRepository.findAll().forEach(detailsList::add);
            for (Details details : detailsList) {
                File file = new File(fileManager.getOptionalPath(true) + details.getStoragePath());
                futures.add(threadPool.submit(() -> {
                    byte[] md5 = fileManager.getFileMD5(file);
                    details.setMd5(md5);
                    String comment;
                    if (file.getAbsolutePath().length() > 25) {
                        comment = "..." + file.getAbsolutePath().substring(file.getAbsolutePath().length() - 20);
                    } else {
                        comment = file.getAbsolutePath();
                    }
                    this.updateMessage("Расчет метаинформации завершен для файла: " + comment);
                    this.updateProgress(count.incrementAndGet(), detailsList.size());
                }));
            }
            checkFutures(futures);
            this.updateMessage("Сохранение данных");
            this.updateProgress(1, 1);
            detailsRepository.saveAll(detailsList);
            this.updateMessage("Расчет успешно завершен");
            threadPool.shutdownNow();
            return true;
        }
    }

    private class FindActivePluginByMD5Task extends Task<Boolean> {

        @Override
        protected Boolean call() {
            ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
            for (Group group : groupRepository.findAllByOrderByValue()) {
                this.updateProgress(0, 0);
                this.updateMessage("Поиск активной конфигурации для " + group.getValue());
                List<Future> futures = new ArrayList<>();
                AtomicInteger count = new AtomicInteger();
                AtomicInteger current = new AtomicInteger();
                List<Release> releases = releaseRepository.findAllByGroup(group);
                for (Release release : releases) {
                    futures.add(threadPool.submit(() -> {
                        List<Details> details = detailsRepository.findAllByRelease(release);
                        count.addAndGet(details.size());
                        boolean apply = true;
                        for (int i = 0; i < details.size(); i++) {
                            if (!fileManager.checkFileMD5(details.get(i))) {
                                apply = false;
                                current.addAndGet(details.size() - i);
                                release.setApplied(false);
                                break;
                            } else {
                                this.updateProgress(current.incrementAndGet(), count.get());
                            }
                        }
                        if (apply) {
                            release.setApplied(true);
                        }
                    }));
                    checkFutures(futures);
                }
                this.updateProgress(1, 1);
                this.updateMessage("Сохранение данных");
                releaseRepository.saveAll(releases);
            }
            this.updateProgress(1, 1);
            this.updateMessage("");
            threadPool.shutdownNow();
            return null;
        }
    }
}