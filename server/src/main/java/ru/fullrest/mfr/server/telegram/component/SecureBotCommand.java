package ru.fullrest.mfr.server.telegram.component;

import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.telegram.TelegramBot;

@Log4j2
public abstract class SecureBotCommand extends BotCommand {

    protected final TelegramUserService telegramUserService;
    private final UserRole accessRole;

    /**
     * Construct a command
     *
     * @param commandIdentifier   the unique identifier of this command (e.g. the command string to
     *                            enter into chat)
     * @param description         the description of this command
     * @param telegramUserService repository with user for check rules
     * @param accessRole          role with access to start command
     */
    public SecureBotCommand(String commandIdentifier, String description, TelegramUserService telegramUserService, UserRole accessRole) {
        super(commandIdentifier, description);
        this.telegramUserService = telegramUserService;
        this.accessRole = accessRole;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        TelegramUser telegramUser = telegramUserService.findById(user.getId());
        if (telegramUser != null) {
            if (telegramUser.getRole() == accessRole) {
                try {
                    execute((TelegramBot) absSender, user, telegramUser, chat, arguments);
                } catch (TelegramApiException e) {
                    log.error(e);
                }
            } else {
                try {
                    absSender.execute(new SendMessage(chat.getId(), "У вас недостаточно прав на выполнение этой операции"));
                } catch (TelegramApiException e) {
                    log.error(e);
                }
            }
        } else {
            try {
                absSender.execute(new SendMessage(chat.getId(), "You are not registered. Press /start"));
            } catch (TelegramApiException e) {
                log.error(e);
            }
        }
    }

    protected abstract void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException;
}
