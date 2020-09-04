package ru.fullrest.mfr.server.telegram.component;

import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.TelegramUserService;

import javax.validation.constraints.NotNull;

@Log4j2
public abstract class SecureCallbackModule {

    public abstract UserRole getAccessRole();

    public abstract TelegramUserService getTelegramUserService();

    public abstract String getName();

    public BotApiMethod<?> init(Chat chat, TelegramUser user){
        if (user != null && user.getRole() == getAccessRole()) {
            return init(chat);
        }
        return null;
    }

    protected abstract BotApiMethod<?> init(Chat chat);

    public BotApiMethod<?> execute(@NotNull CallbackQuery callbackQuery, @NotNull CallbackData callbackData) {
        Integer userId = callbackQuery.getFrom().getId();
        TelegramUser user = getTelegramUserService().findById(userId);
        if (user != null && user.getRole() == getAccessRole()) {
            return process(callbackQuery, callbackData);
        }
        return new AnswerCallbackQuery().setCallbackQueryId(callbackQuery.getId());
    }

    protected abstract BotApiMethod<?> process(@NotNull CallbackQuery callbackQuery, @NotNull CallbackData callbackData);
}
