package ru.fullrest.mfr.server.telegram.command;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.telegram.TelegramBot;
import ru.fullrest.mfr.server.telegram.callback_module.UserRoleCallbackModule;
import ru.fullrest.mfr.server.telegram.component.SecureBotCommand;

@Log4j2
@Component
public class SetUserRoleCommand extends SecureBotCommand {

    private final UserRoleCallbackModule userRoleCallbackModule;

    public SetUserRoleCommand(TelegramUserService telegramUserService, UserRoleCallbackModule userRoleCallbackModule) {
        super("setRole", "Set role to user", telegramUserService, UserRole.ADMIN);
        this.userRoleCallbackModule = userRoleCallbackModule;
    }

    @Override
    public void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        absSender.execute(userRoleCallbackModule.init(chat, telegramUser));
    }
}
