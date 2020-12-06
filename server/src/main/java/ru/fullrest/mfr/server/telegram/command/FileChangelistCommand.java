package ru.fullrest.mfr.server.telegram.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.api.GameUpdate;
import ru.fullrest.mfr.api.MoveFile;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.service.updater.UpdaterProcessorExecutor;
import ru.fullrest.mfr.server.telegram.TelegramBot;
import ru.fullrest.mfr.server.telegram.component.SecureBotCommand;

@Component
public class FileChangelistCommand extends SecureBotCommand {
    private final UpdaterProcessorExecutor updaterProcessorExecutor;


    public FileChangelistCommand(TelegramUserService telegramUserService, UpdaterProcessorExecutor updaterProcessorExecutor) {
        super("filechangelist", telegramUserService, UserRole.ADMIN);
        this.updaterProcessorExecutor = updaterProcessorExecutor;
    }

    @Override
    protected void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        StringBuilder builder = new StringBuilder();
        final GameUpdate update = updaterProcessorExecutor.getStatus().getUpdate();
        if (!update.getAddFiles().isEmpty()) {
            builder.append("Добавить файлы: \n");
            for (String file : update.getAddFiles()) {
                if (builder.length() + file.length() > 4095) { //safe with \n
                    absSender.execute(new SendMessage().setChatId(chat.getId()).setText(builder.toString()));
                    builder = new StringBuilder("Добавить файлы: \n");
                }
                builder.append(file).append("\n");
            }
            absSender.execute(new SendMessage().setChatId(chat.getId()).setText(builder.toString()));
        }
        if (!update.getRemoveFiles().isEmpty()) {
            builder.append("Удалить файлы: \n");
            for (String file : update.getRemoveFiles()) {
                if (builder.length() + file.length() > 4095) {
                    absSender.execute(new SendMessage().setChatId(chat.getId()).setText(builder.toString()));
                    builder = new StringBuilder("Удалить файлы: \n");
                }
                builder.append(file).append("\n");
            }
            absSender.execute(new SendMessage().setChatId(chat.getId()).setText(builder.toString()));
        }

        if (!update.getMoveFiles().isEmpty()) {
            builder.append("Переместить файлы: \n");
            for (MoveFile file : update.getMoveFiles()) {
                if (builder.length() + file.toString().length() > 4095) {
                    absSender.execute(new SendMessage().setChatId(chat.getId()).setText(builder.toString()));
                    builder = new StringBuilder("Переместить файлы: \n");
                }
                builder.append(file).append("\n");
            }
            absSender.execute(new SendMessage().setChatId(chat.getId()).setText(builder.toString()));
        }
    }
}
