package ru.fullrest.mfr.plugins_configuration_utility.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
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
import ru.fullrest.mfr.plugins_configuration_utility.exception.ApplicationUpdateException;
import ru.fullrest.mfr.plugins_configuration_utility.exception.GameUpdateException;
import ru.fullrest.mfr.plugins_configuration_utility.exception.RestException;
import ru.fullrest.mfr.plugins_configuration_utility.javafx.View;
import ru.fullrest.mfr.plugins_configuration_utility.manager.FileManager;
import ru.fullrest.mfr.plugins_configuration_utility.manager.StageManager;
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

    @Autowired
    private View<AlertController> alertView;

    @Autowired
    private View<AlertNewsController> alertNewsView;

    @Autowired
    private StageManager stageManager;

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
        checkApplicationUpdateTask.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, successEvent -> {
            if (checkApplicationUpdateTask.getValue()) {
                Button ok = new Button("Да");
                Button cancel = new Button("Нет");
                String text = "Доступно новая версия Plugin Configuration Utility\nХотите установить?";
                alertView.getController().createAlert(text, cancel, ok);
                ok.setOnAction(okEvent -> {
                    ApplicationUpdateTask applicationUpdateTask = new ApplicationUpdateTask();
                    headerLabel.setText("Обновляем конфигуратор");
                    applicationUpdateTask
                            .addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> closeButtonAction());
                    applicationUpdateTask.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED, event ->
                            exceptionHandler("Ошибка обновления конфигуратора", checkApplicationUpdateTask
                                    .getException()));
                    new Thread(applicationUpdateTask).start();
                    stageManager.getAlertStage().close();
                });
                cancel.setOnAction(event -> {
                    stageManager.getAlertStage().close();
                    closeButtonAction();
                });
                stageManager.getAlertStage().showAndWait();
            } else {
                closeButtonAction();
            }
        });
        checkApplicationUpdateTask.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED, failedEvent ->
                exceptionHandler("Ошибка проверки обновлений для конфигуратора", checkApplicationUpdateTask
                        .getException()));
        new Thread(checkApplicationUpdateTask).start();
    }

    public void checkGameUpdate(String version, boolean silent) {
        CheckGameUpdateTask task = new CheckGameUpdateTask(version);
        headerLabel.setText("Проверка обновлений игры");
        closeButton.setVisible(false);
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            Version result = task.getValue();
            String[] split = version.trim().split("\\.");
            boolean latVersion = false;
            if (split.length >= 3) {
                if (split[0].equals("3")) {
                    if (split[1].equals("1")) {
                        if (split[2].equals("09")) {
                            latVersion = true;
                            Button ok = new Button("Ок");
                            String text = "Что ж, уважаемые любители Morrowind, вот и наступил новый виток жизни " +
                                    "проекта M[FR]! \n" + "В связи с многочисленными изменениями, такими как новые " +
                                    "движки и оптимизация их запуска из одной папки, обновление необходимо скачать " +
                                    "вручную. \n" + "Для обновления с версии 3.1.09 или более ранней, необходимо " +
                                    "скачать свежий инсталятор игры от 03.11.2019!\n" + "Не все идеи были " +
                                    "реализованы, в данный момент упор был сделан на ликвидацию всех найденных багов " +
                                    "за многолетнюю историю проекта. Но и без нововведений мы вас не оставили, со " +
                                    "списком свежего контента можно ознакомиться в ченджлоге.";
                            alertNewsView.getController().createAlert(text, ok);
                            ok.setOnAction(okEvent -> stageManager.getAlertNewsStage().close());
                            stageManager.getAlertNewsStage().showAndWait();
                            closeButtonAction();
                        }
                    }
                }
            }
            if (!latVersion) {
                if (result.isNeedUpdate() && result.getUpdatePlan() != null) {
                    Button ok = new Button("Да");
                    Button cancel = new Button("Нет");
                    String text = "Доступна новая версия M[FR]\nХотите установить?";
                    alertView.getController().createAlert(text, cancel, ok);
                    ok.setOnAction(okEvent -> {
                        GameUpdateTask gameUpdateTask = new GameUpdateTask(result);
                        headerLabel.setText("Установка обновления");
                        gameUpdateTask.addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, successEvent -> {
                            if (propertiesConfiguration.isRefreshSchema()) {
                                refreshSchema();
                            } else {
                                if (propertiesConfiguration.isRefreshApplies()) {
                                    refreshApplied(false);
                                } else {
                                    closeButtonAction();
                                }
                            }
                        });
                        gameUpdateTask.addEventFilter(WorkerStateEvent.WORKER_STATE_FAILED,
                                failedEvent -> exceptionHandler("Ошибка обновления M[FR]",
                                        gameUpdateTask.getException()));
                        new Thread(gameUpdateTask).start();
                        stageManager.getAlertStage().close();
                    });
                    cancel.setOnAction(cancelEvent -> {
                        stageManager.getAlertStage().close();
                        closeButtonAction();
                    });
                    stageManager.getAlertStage().showAndWait();
                } else {
                    if (!silent) {
                        Button ok = new Button("Ок");
                        String text = "Установлена последняя версия";
                        alertView.getController().createAlert(text, ok);
                        ok.setOnAction(okEvent -> stageManager.getAlertStage().close());
                        stageManager.getAlertStage().showAndWait();
                    }
                    closeButtonAction();
                }
            }
        });
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event ->
                exceptionHandler("Ошибка проверки обновлений для M[FR]", task.getException()));
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

    private void refreshSchema() {
        File schema = fileManager.getSchemaFile();
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

    private void refreshApplied(boolean findActivePlugins) {
        headerLabel.setText("Обновление данных MD5");
        CalculateMD5Task calculateMD5Task = new CalculateMD5Task();
        calculateMD5Task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, calculateMD5TaskEvent -> {
            headerLabel.setText("Переподключение активной конфигурации");
            List<Release> activeReleases = releaseRepository.findAllByAppliedIsTrue();
            EnableReleasesTask enableReleasesTask = new EnableReleasesTask(activeReleases);
            enableReleasesTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, enableReleasesTaskEvent -> {
                if (findActivePlugins) {
                    headerLabel.setText("Проверка активной конфигурации");
                    FindActivePluginByMD5Task findActivePluginByMD5Task =
                            new FindActivePluginByMD5Task();
                    findActivePluginByMD5Task
                            .addEventFilter(WorkerStateEvent.WORKER_STATE_SUCCEEDED, findActivePluginByMD5TaskEvent -> {
                                headerLabel.setText("Обновление успешно завершено");
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

    public void closeButtonAction() {
        stageManager.getProgressStage().close();
    }

    private void exceptionHandler(String text, Throwable throwable) {
        log.error(throwable);
        headerLabel.setText(text);
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0);
        closeButton.setDisable(false);
        closeButton.setVisible(true);
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
        protected Boolean call() throws RestException, ApplicationUpdateException {
            HttpHeaders headers = new HttpHeaders();
            headers.add("version", propertiesConfiguration.getApplicationVersion());
            headers.add("platform", propertiesConfiguration.getPlatform());
            ResponseEntity<Version> result;
            try {
                result = restTemplate.exchange(String.format("%s%s%s", propertiesConfiguration.getUpdateLink(),
                        Links.PUBLIC_API_LINK, Links.PUBLIC_API_APPLICATION_VERSION_LINK),
                        HttpMethod.GET, new HttpEntity<>(headers), Version.class);
            } catch (ResourceAccessException e) {
                this.updateMessage("Не удалось подключиться к серверу обновлений");
                throw new RestException("Can't connect to server", e);
            }
            if (result != null && result.getBody() != null) {
                if (result.getBody().isClientVersionIsDefined()) {
                    if (result.getBody().isNeedUpdate()) {
                        this.updateMessage("Доступно обновление для скачивания");
                        return true;
                    } else {
                        this.updateMessage("Установлена последняя версия");
                        return false;
                    }
                } else {
                    this.updateMessage("Операционная система не определена");
                    throw new ApplicationUpdateException("Application client platform is not found on server! " +
                            "Platform: " + propertiesConfiguration.getPlatform());
                }
            } else {
                this.updateMessage("Некорректный ответ сервера");
                throw new ApplicationUpdateException("Application update version is null.\n" + result);
            }
        }
    }

    private class ApplicationUpdateTask extends InnerTask<Void> {

        @Override
        protected Void call() throws ApplicationUpdateException {
            ResponseEntity<byte[]> responseEntity;
            try {
                responseEntity = restTemplate.exchange(
                        String.format("%s%s%s%s", propertiesConfiguration.getUpdateLink(),
                                Links.PUBLIC_API_LINK, Links.PUBLIC_API_APPLICATION_UPDATE_LINK,
                                propertiesConfiguration.getPlatform()),
                        HttpMethod.GET, null, byte[].class);
            } catch (ResourceAccessException e) {
                this.updateMessage("Не удалось подключиться к серверу обновлений");
                throw new ApplicationUpdateException("Can't connect to server");
            }
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                if (fileManager.downloadAndUpdateApplication(responseEntity.getBody())) {
                    System.exit(0);
                    return null;
                } else {
                    this.updateMessage("Не удалось обновить конфигуратор");
                    throw new ApplicationUpdateException("Can't install new application");
                }
            } else {
                this.updateMessage("Не удалось скачать обновление");
                throw new ApplicationUpdateException("Can't download update " + propertiesConfiguration.getPlatform());
            }
        }
    }

    @RequiredArgsConstructor
    private class CheckGameUpdateTask extends InnerTask<Version> {

        private final String version;

        @Override
        protected Version call() throws GameUpdateException, RestException {
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
                this.updateMessage("Не удалось подключиться к серверу обновлений");
                throw new RestException("Can't connect to server");
            }
            if (result != null && result.getBody() != null) {
                if (result.getBody().isClientVersionIsDefined()) {
                    return result.getBody();
                } else {
                    this.updateMessage("Версия игры не найдена на сервере");
                    throw new GameUpdateException("Game version is not found on server! Version: " + result);
                }

            } else {
                this.updateMessage("Некорректный ответ сервера");
                throw new GameUpdateException("Game update version is null.\n" + result);
            }
        }
    }

    @RequiredArgsConstructor
    private class GameUpdateTask extends InnerTask<Void> {

        private final Version version;

        @Override
        protected Void call() throws RestException, GameUpdateException {
            propertiesConfiguration.setRefreshSchema(version.getUpdatePlan().isRefreshSchema());
            propertiesConfiguration.setRefreshApplies(version.getUpdatePlan().isRefreshApplied());
            for (int i = 0; i < version.getUpdatePlan().getUpdates().size(); i++) {
                this.updateProgress(i, version.getUpdatePlan().getUpdates().size());
                String update = version.getUpdatePlan().getUpdates().get(i);
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
                    this.updateMessage("Не удалось подключиться к серверу обновлений");
                    throw new RestException("Can't connect to server");
                }
                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    this.updateMessage("Установка обновления " + update);
                    if (!fileManager.downloadAndApplyGamePatch(responseEntity.getBody())) {
                        this.updateMessage("Не удалось установить обновление " + update);
                        throw new GameUpdateException("Can't install patch " + update);
                    }
                } else {
                    this.updateMessage("Не удалось скачать обновление " + update);
                    throw new GameUpdateException("Can't download update " + update);
                }
            }
            this.updateProgress(1, 1);
            return null;
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