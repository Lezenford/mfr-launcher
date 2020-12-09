package ru.fullrest.mfr.server.telegram.command;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.telegram.TelegramBot;
import ru.fullrest.mfr.server.telegram.callback_module.StatisticsCallbackModule;
import ru.fullrest.mfr.server.telegram.component.SecureBotCommand;

@Log4j2
@Component
public class StatisticsCommand extends SecureBotCommand {

    private final StatisticsCallbackModule module;

    public StatisticsCommand(TelegramUserService telegramUserService, StatisticsCallbackModule module) {
        super("statistics", telegramUserService, UserRole.ADMIN);
        this.module = module;
    }

    @Override
    protected void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        BotApiMethod<?> init = module.init(chat, telegramUser);
        if (init != null) {
            absSender.execute(init);
        }
    }
}
