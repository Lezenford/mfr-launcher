package ru.fullrest.mfr.server.service.updater;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.fullrest.mfr.api.GameUpdate;

@Getter
@RequiredArgsConstructor
public class Status {
    private final boolean running;
    private final String operation;
    private final Exception error;
    private final GameUpdate update;

    @Override
    public String toString() {
        return String.format(
                "Консоль управления.\n\nТекущее состояние: %s\nТекущая операция: %s\nОшибка последней операции: %s\n\n" +
                        "Версия: %s\nСписок изменений: %s\nСостав файлов: %s",
                running ? "Выполняется операция" : "Операция завершена",
                operation,
                error != null ? error.getMessage() : "",
                update.getVersion(),
                !update.getChangeLog().isEmpty() ? "/changelist" : "",
                !update.getRemoveFiles().isEmpty() || !update.getMoveFiles().isEmpty() || !update.getAddFiles().isEmpty() ? "/filechangelist" : ""
                            );
    }
}
