package ru.fullrest.mfr.server.telegram.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.api.GameUpdate;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.service.updater.UpdaterProcessorExecutor;
import ru.fullrest.mfr.server.telegram.TelegramBot;
import ru.fullrest.mfr.server.telegram.component.SecureBotCommand;

@Component
public class TextChangelistCommand extends SecureBotCommand {
    private final UpdaterProcessorExecutor updaterProcessorExecutor;

    public TextChangelistCommand(TelegramUserService telegramUserService, UpdaterProcessorExecutor updaterProcessorExecutor) {
        super("changelist", telegramUserService, UserRole.ADMIN);
        this.updaterProcessorExecutor = updaterProcessorExecutor;
    }

    @Override
    protected void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        StringBuilder builder = new StringBuilder();
        final GameUpdate update = updaterProcessorExecutor.getStatus().getUpdate();
        if (!update.getChangeLog().isBlank()) {
            builder.append("Список изменений: \n");
            final String[] words = update.getChangeLog().split(" ");
            for (String word : words) {
                if (builder.length() + word.length() > 4095) { //safe with \n
                    absSender.execute(new SendMessage().setChatId(chat.getId()).setText(builder.toString()));
                    builder = new StringBuilder("Список изменений: \n");
                }
                builder.append(word).append(" ");
            }
            absSender.execute(new SendMessage().setChatId(chat.getId()).setText(builder.toString()));
        }
    }
}
