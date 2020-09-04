package ru.fullrest.mfr.server.telegram.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.telegram.TelegramBot;
import ru.fullrest.mfr.server.telegram.component.SecureBotCommand;

@Component
public class ExitBotCommand extends SecureBotCommand {
    /**
     * Construct a command
     *
     * @param telegramUserService repository with user for check rules
     */
    public ExitBotCommand(TelegramUserService telegramUserService) {
        super("exit", "remove user from bot", telegramUserService, UserRole.USER);
    }

    @Override
    public void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        telegramUserService.delete(telegramUser);
        absSender.execute(new SendMessage(chat.getId(), "Your account successfully deleted from bot"));
    }
}
