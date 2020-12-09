package ru.fullrest.mfr.server.service.updater;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import ru.fullrest.mfr.api.GameUpdate;
import ru.fullrest.mfr.server.common.Function;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.service.UpdateService;
import ru.fullrest.mfr.server.service.updater.event.OperationUpdateEvent;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Service
@RequiredArgsConstructor
public class UpdaterProcessorExecutor {
    private final GitService gitService;
    private final FileService fileService;
    private final UpdateService updateService;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final GameUpdate EMPTY_GAME_UPDATE =
            new GameUpdate("", Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), "");

    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> operation = new AtomicReference<>("");
    private final AtomicReference<Exception> error = new AtomicReference<>(null);
    private final AtomicReference<GameUpdate> update = new AtomicReference<>(EMPTY_GAME_UPDATE);

    public Status getStatus() {
        return new Status(running.get(), operation.get(), error.get(), update.get());
    }

    public boolean repositoryExist() {
        return gitService.repositoryExist();
    }

    public void cloneRepository() {
        run(() -> {
            gitService.cloneRepository();
            update.set(EMPTY_GAME_UPDATE);
        }, "Загрузка данных");
    }

    public void updateRepository() {
        run(() -> {
            gitService.updateRepository();
            update.set(EMPTY_GAME_UPDATE);
        }, "Обновление данных");
    }

    public void setChangeLog(String changeLog) {
        run(() -> update.getAndUpdate(
                gameUpdate -> gameUpdate.copy(
                        gameUpdate.getVersion(),
                        gameUpdate.getAddFiles(),
                        gameUpdate.getMoveFiles(),
                        gameUpdate.getRemoveFiles(),
                        changeLog
                                             )), "Установка списка изменений"
           );
    }

    public void prepareUpdate() {
        run(() -> update.set(fileService.checkVersion(gitService.getDiffForPatch())), "Подготовка патча");
    }

    public void createUpdate() {
        run(() -> {
            final File patch = fileService.createPatch(update.get());
            try {
                Update update = new Update();
                update.setActive(true);
                update.setPath(patch.getName());
                update.setVersion(this.update.get().getVersion());
                updateService.save(update);
            } catch (Exception e) {
                Files.deleteIfExists(patch.toPath());
                throw e;
            }
            try {
                gitService.setTag(update.get().getVersion());
            } finally {
                this.update.set(EMPTY_GAME_UPDATE);
            }
        }, "Создание патча");
    }

    private void run(Function function, String operation) {
        log.info("Try to start operation " + operation);
        if (running.compareAndSet(false, true)) {
            this.operation.set(operation);
            error.set(null);
            log.info("Operation " + operation + " successfully run");
            applicationEventPublisher.publishEvent(new OperationUpdateEvent(this));
            threadPool.execute(() -> {
                try {
                    function.call();
                    log.info("Operation successfully finish");
                } catch (Exception e) {
                    error.set(e);
                    log.error("Operation has error", e);
                } finally {
                    running.set(false);
                    applicationEventPublisher.publishEvent(new OperationUpdateEvent(this));
                }
            });
        } else {
            log.info("Operation" + this.operation.get() + " already running");
        }
    }
}
