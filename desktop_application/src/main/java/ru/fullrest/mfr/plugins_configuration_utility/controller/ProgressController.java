package ru.fullrest.mfr.plugins_configuration_utility.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.fullrest.mfr.common.Links;
import ru.fullrest.mfr.common.Version;
import ru.fullrest.mfr.plugins_configuration_utility.config.PropertiesConfiguration;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
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
    private PropertiesConfiguration propertiesConfiguration;

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

    private RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(30))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public void beforeOpen() {
        closeButton.setDisable(true);
    }

    public void checkApplicationUpdate() {
        CheckApplicationUpdateTask checkApplicationUpdateTask = new CheckApplicationUpdateTask();
        headerLabel.setText("Проверяем обновление конфигуратора");
        closeButton.setVisible(false);
        checkApplicationUpdateTask
                .addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, checkApplicationUpdateTaskEvent -> {
                    if (checkApplicationUpdateTask.getValue()) {
                        closeButtonAction();
                    } else {
                        headerLabel.setText("Ошибка обновления конфигуратора");
                        progressBar.progressProperty().unbind();
                        progressBar.setProgress(0);
                        closeButton.setDisable(false);
                        closeButton.setVisible(true);
                    }
                });
        new Thread(checkApplicationUpdateTask).start();
    }

    public void checkGameUpdate(String version) {
        CheckGameUpdateTask task = new CheckGameUpdateTask(version);
        headerLabel.setText("Проверка обновлений игры");
        closeButton.setVisible(false);
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            if (task.getValue()) {
                headerLabel.setText("Обновление успешно завершено");
                if (propertiesConfiguration.isRefreshSchema()) {
                    File schema = fileManager.getSchemaFile();
                    if (schema != null) {
                        headerLabel.setText("Обновление конфигурации");
                        List<Release> activeReleases = releaseRepository.findAllByAppliedIsTrue();
                        groupRepository.deleteAll();
                        ReadJsonTask readJsonTask = new ReadJsonTask(schema);
                        readJsonTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, readJsonTaskEvent -> {
                            if (readJsonTask.getValue()) {
                                headerLabel.setText("Вычисляем активные плагины из прошлой конфигурации");
                                List<Release> newReleases = (List<Release>) releaseRepository.findAll();
                                Comparator<Release> comparator = (o1, o2) -> {
                                    if (o1.getValue().equals(o2.getValue()) &&
                                            o1.getGroup().getValue().equals(o2.getGroup().getValue())) {
                                        return 0;
                                    }
                                    return -1;
                                };
                                newReleases.forEach(release ->
                                        activeReleases.forEach(activeRelease -> {
                                            if (comparator.compare(release, activeRelease) == 0) {
                                                release.setApplied(true);
                                            }
                                        }));
                                headerLabel.setText("Сохранение данных");
                                releaseRepository.saveAll(newReleases);
                                refreshApplied(true);
                            } else {
                                headerLabel.setText("Не удалось прочитать новый файл схемы");
                                closeButton.setVisible(true);
                                closeButton.setDisable(false);
                            }
                        });
                        new Thread(readJsonTask).start();
                    }
                } else {
                    if (propertiesConfiguration.isRefreshApplies()) {
                        refreshApplied(false);
                    } else {
                        closeButtonAction();
                    }
                }
            } else {
                closeButton.setVisible(true);
                closeButton.setDisable(false);
                headerLabel.setText("Ошибка обновления!");
                progressBar.progressProperty().unbind();
                progressBar.setProgress(0);
            }
        });
        new Thread(task).start();
    }

    public void acceptPluginChanges(Map<Group, Release> groupMap) {
        headerLabel.setText("Применение изменений");
        closeButton.setVisible(false);
        List<Release> disabledReleases = new ArrayList<>();
        groupMap.forEach((group, release) -> {
            Release appliedRelease = releaseRepository.findFirstByGroupAndAppliedIsTrue(group);
            if (appliedRelease != null && !appliedRelease.equals(release)) {
                disabledReleases.add(appliedRelease);
            }
        });
        DisableReleasesTask disableReleasesTask = new DisableReleasesTask(disabledReleases);
        disableReleasesTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            List<Release> enabledReleases = new ArrayList<>();
            groupMap.forEach((group, release) -> {
                Release appliedRelease = releaseRepository.findFirstByGroupAndAppliedIsTrue(group);
                if (appliedRelease == null) {
                    enabledReleases.add(release);
                }
            });
            EnableReleasesTask enableReleasesTask = new EnableReleasesTask(enabledReleases);
            enableReleasesTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event1 -> {
                headerLabel.setText("Обновление завершено");
                closeButton.setDisable(false);
                closeButton.setVisible(true);
            });
            new Thread(enableReleasesTask).start();
        });
        new Thread(disableReleasesTask).start();
    }

    public void readJson(File file) {
        ReadJsonTask readJsonTask = new ReadJsonTask(file);
        closeButton.setVisible(false);
        headerLabel.setText("Анализ конфигурации");
        readJsonTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, readJsonTaskEvent -> {
            if (readJsonTask.getValue()) {
                headerLabel.setText("Загрузка завершена");
                CalculateMD5Task calculateMD5Task = new CalculateMD5Task();
                headerLabel.setText("Анализ опциональных файлов");
                calculateMD5Task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, calculateMD5TaskEvent -> {
                    headerLabel.setText("Анализ файлов завершен");
                    FindActivePluginByMD5Task findActivePluginByMD5Task = new FindActivePluginByMD5Task();
                    headerLabel.setText("Поиск активированных плагинов");
                    findActivePluginByMD5Task
                            .addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, findActivePluginTaskEvent -> {
                                headerLabel.setText("Поиск активной конфигурации завершен");
                                closeButton.setVisible(true);
                                closeButton.setDisable(false);
                            });
                    new Thread(findActivePluginByMD5Task).start();
                });
                new Thread(calculateMD5Task).start();
            } else {
                headerLabel.setText("Ошибка загрузки");
                closeButton.setVisible(true);
                closeButton.setDisable(false);
            }

        });
        new Thread(readJsonTask).start();
    }

    public void closeButtonAction() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

    private void refreshApplied(boolean findActivePlugins) {
        headerLabel.setText("Обновление данных MD5");
        CalculateMD5Task calculateMD5Task = new CalculateMD5Task();
        calculateMD5Task
                .addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                        calculateMD5TaskEvent -> {
                            headerLabel.setText("Переподключение активной конфигурации");
                            List<Release> activeReleases = releaseRepository
                                    .findAllByAppliedIsTrue();
                            EnableReleasesTask enableReleasesTask =
                                    new EnableReleasesTask(activeReleases);
                            enableReleasesTask
                                    .addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                                            enableReleasesTaskEvent -> {
                                                if (findActivePlugins) {
                                                    headerLabel
                                                            .setText("Проверка активной конфигурации");
                                                    FindActivePluginByMD5Task findActivePluginByMD5Task =
                                                            new FindActivePluginByMD5Task();
                                                    findActivePluginByMD5Task
                                                            .addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED,
                                                                    findActivePluginByMD5TaskEvent -> {
                                                                        headerLabel
                                                                                .setText("Обновление успешно " +
                                                                                        "завершено");
                                                                        closeButton.setDisable(false);
                                                                        closeButton.setVisible(true);
                                                                    });
                                                    new Thread(findActivePluginByMD5Task).start();
                                                } else {
                                                    headerLabel.setText("Обновление успешно завершено");
                                                    closeButton.setDisable(false);
                                                    closeButton.setVisible(true);
                                                }
                                            });
                            new Thread(enableReleasesTask).start();
                        });
        new Thread(calculateMD5Task).start();
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

    private abstract class InnerTask<K> extends Task<K> {

        InnerTask() {
            progressBar.progressProperty().unbind();
            progressBar.progressProperty().bind(this.progressProperty());
            informationLabel.textProperty().unbind();
            informationLabel.textProperty().bind(this.messageProperty());
        }
    }

    @RequiredArgsConstructor
    private class ReadJsonTask extends InnerTask<Boolean> {

        private final File file;

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
                return false;
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

    private class CalculateMD5Task extends InnerTask<Void> {

        @Override
        protected Void call() {
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
            return null;
        }
    }

    private class FindActivePluginByMD5Task extends InnerTask<Void> {

        @Override
        protected Void call() {
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

    private class CheckApplicationUpdateTask extends InnerTask<Boolean> {

        @Override
        protected Boolean call() {
            HttpHeaders headers = new HttpHeaders();
            headers.add("version", propertiesConfiguration.getApplicationVersion());
            headers.add("platform", propertiesConfiguration.getPlatform());
            ResponseEntity<Version> result;
            try {
                result = restTemplate
                        .exchange(String.format("%s%s%s", propertiesConfiguration
                                        .getUpdateLink(), Links.PUBLIC_API_LINK,
                                Links.PUBLIC_API_APPLICATION_VERSION_LINK),
                                HttpMethod.GET, new HttpEntity<>(headers),
                                Version.class);
            } catch (ResourceAccessException e) {
                log.info("Can't connect to server");
                this.updateMessage("Не удалось подключиться к серверу обновлений");
                return false;
            }
            if (result != null && result.getBody() != null) {
                if (result.getBody().isClientVersionIsDefined()) {
                    if (result.getBody().isNeedUpdate()) {
                        ResponseEntity<byte[]> responseEntity;
                        try {
                            responseEntity = restTemplate.exchange(
                                    String.format("%s%s%s%s", propertiesConfiguration.getUpdateLink(),
                                            Links.PUBLIC_API_LINK, Links.PUBLIC_API_APPLICATION_UPDATE_LINK,
                                            propertiesConfiguration.getPlatform()),
                                    HttpMethod.GET, null, byte[].class);
                        } catch (ResourceAccessException e) {
                            log.info("Can't connect to server");
                            this.updateMessage("Не удалось подключиться к серверу обновлений");
                            return false;
                        }
                        if (responseEntity.getStatusCode() == HttpStatus.OK) {
                            if (fileManager.downloadAndUpdateApplication(responseEntity.getBody())) {
                                System.exit(0);
                            } else {
                                log.error("Can't install new application");
                                this.updateMessage("Не удалось обновить конфигуратор");
                                return false;
                            }
                        } else {
                            log.error("Can't download update " + propertiesConfiguration.getPlatform());
                            this.updateMessage("Не удалось скачать обновление");
                            return false;
                        }

                    } else {
                        this.updateMessage("Установлена последняя версия");
                    }
                } else {
                    log.error("Application client platform is not found on server! Platform: " + propertiesConfiguration
                            .getPlatform());
                    this.updateMessage("Операционная система не определена");
                    return false;
                }
            } else {
                log.error("Application update result is null.\n" + result);
                this.updateMessage("Некорректный ответ сервера");
                return false;
            }
            return true;
        }
    }

    @RequiredArgsConstructor
    private class CheckGameUpdateTask extends InnerTask<Boolean> {

        private final String version;

        @Override
        protected Boolean call() {
            this.updateMessage("");
            HttpHeaders headers = new HttpHeaders();
            headers.add("version", version);
            headers.add("platform", propertiesConfiguration.getGamePlatform());
            ResponseEntity<Version> result;
            try {
                result = restTemplate
                        .exchange(String.format("%s%s%s", propertiesConfiguration
                                        .getUpdateLink(), Links.PUBLIC_API_LINK, Links.PUBLIC_API_GAME_VERSION_LINK),
                                HttpMethod.GET, new HttpEntity<>(headers),
                                Version.class);
            } catch (ResourceAccessException e) {
                log.info("Can't connect to server");
                this.updateMessage("Не удалось подключиться к серверу обновлений");
                return false;
            }
            if (result != null && result.getBody() != null) {
                if (result.getBody().isClientVersionIsDefined()) {
                    if (result.getBody().isNeedUpdate() && result.getBody().getUpdatePlan() != null) {
                        propertiesConfiguration.setRefreshSchema(result.getBody().getUpdatePlan().isRefreshSchema());
                        propertiesConfiguration.setRefreshApplies(result.getBody().getUpdatePlan().isRefreshApplied());
                        for (int i = 0; i < result.getBody().getUpdatePlan().getUpdates().size(); i++) {
                            this.updateProgress(i, result.getBody().getUpdatePlan().getUpdates().size());
                            String update = result.getBody().getUpdatePlan().getUpdates().get(i);
                            this.updateMessage("Скачивание обновления " + update);
                            ResponseEntity<byte[]> responseEntity;
                            try {
                                responseEntity = restTemplate
                                        .exchange(String.format("%s%s%s/%s/%s", propertiesConfiguration
                                                        .getUpdateLink(), Links.PUBLIC_API_LINK,
                                                Links.PUBLIC_API_GAME_UPDATE_LINK, propertiesConfiguration
                                                        .getGamePlatform(), update),
                                                HttpMethod.GET, null, byte[].class);
                            } catch (ResourceAccessException e) {
                                log.info("Can't connect to server");
                                this.updateMessage("Не удалось подключиться к серверу обновлений");
                                return false;
                            }
                            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                                this.updateMessage("Установка обновления " + update);
                                if (!fileManager.downloadAndApplyGamePatch(responseEntity.getBody())) {
                                    log.error("Can't install patch " + update);
                                    this.updateMessage("Не удалось установить обновление " + update);
                                    return false;
                                }
                            } else {
                                log.error("Can't download update " + update);
                                this.updateMessage("Не удалось скачать обновление " + update);
                                return false;
                            }
                        }
                        this.updateProgress(1, 1);
                        return true;
                    } else {
                        return true;
                    }
                } else {
                    log.error("Game version is not found on server! Version: " + version);
                    this.updateMessage("Версия игры не найдена на сервере");
                }
            } else {
                log.error("Game update result is null.\n" + result);
                this.updateMessage("Некорректный ответ сервера");
            }
            return false;
        }
    }

    @RequiredArgsConstructor
    private class DisableReleasesTask extends InnerTask<Void> {

        private final List<Release> releases;

        @Override
        protected Void call() {
            ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
            releases.forEach(release -> {
                List<Future> futures = new ArrayList<>();
                AtomicInteger count = new AtomicInteger();
                this.updateMessage("Отключение модуля: " + release.getValue());
                this.updateProgress(0, 0);
                List<Details> details = detailsRepository.findAllByRelease(release);
                details.forEach(detail ->
                        futures.add(threadPool.submit(() -> {
                            fileManager.removeFromGameDirectory(detail);
                            this.updateProgress(count.incrementAndGet(), details.size());
                        })));
                checkFutures(futures);
                release.setApplied(false);
            });
            releaseRepository.saveAll(releases);
            this.updateProgress(1, 1);
            this.updateMessage("");
            threadPool.shutdownNow();
            return null;
        }
    }

    @RequiredArgsConstructor
    private class EnableReleasesTask extends InnerTask<Void> {

        private final List<Release> releases;

        @Override
        protected Void call() {
            ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
            releases.forEach(release -> {
                List<Future> futures = new ArrayList<>();
                AtomicInteger count = new AtomicInteger();
                this.updateMessage("Подключение модуля: " + release.getValue());
                this.updateProgress(0, 0);
                List<Details> details = detailsRepository.findAllByRelease(release);
                details.forEach(detail ->
                        futures.add(threadPool.submit(() -> {
                            fileManager.copyToGameDirectory(detail);
                            this.updateProgress(count.incrementAndGet(), details.size());
                        })));
                checkFutures(futures);
                release.setApplied(true);
            });
            releaseRepository.saveAll(releases);
            this.updateProgress(1, 1);
            this.updateMessage("");
            threadPool.shutdownNow();
            return null;
        }
    }
}