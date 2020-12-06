package ru.fullrest.mfr.server.telegram;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.CommandRegistry;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.service.updater.event.SendMessageEvent;
import ru.fullrest.mfr.server.telegram.callback_module.UserRoleCallbackModule;
import ru.fullrest.mfr.server.telegram.component.CallbackAnswer;
import ru.fullrest.mfr.server.telegram.component.CallbackModuleRegistry;

import java.util.concurrent.ConcurrentMap;

@Log4j2
@Getter
@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramWebhookBot implements ApplicationListener<SendMessageEvent> {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.path}")
    private String botPath;

    private final CommandRegistry commandRegistry;
    private final CallbackModuleRegistry callbackModuleRegistry;
    private final ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap;
    private final UserRoleCallbackModule userRoleCallbackModule;

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if (update == null) {
            return null;
        }
        if (update.hasMessage() && update.getMessage().isCommand()) {
            callbackAnswerMap.remove(update.getMessage().getChatId());
            commandRegistry.executeCommand(this, update.getMessage());
            return null;
        }
        if (update.hasCallbackQuery()) {
            try {
                return callbackModuleRegistry.execute(update);
            } catch (TelegramApiException e) {
                log.error(e);
            }
            return null;
//            try {
//                callbackAnswerMap.remove(update.getCallbackQuery().getMessage().getChat().getId());
//                setUserRoleCallback.execute(
//                        this, update.getCallbackQuery().getFrom(), update.getCallbackQuery().getMessage().getChat(), update);
//            } catch (TelegramApiException e) {
//                log.error(e);
//            }
//            return null;
        }
        if (update.hasMessage()) {
            CallbackAnswer callbackAnswer = callbackAnswerMap.get(update.getMessage().getChatId());
            if (callbackAnswer != null) {
                try {
                    callbackAnswer.execute(update);
                } catch (TelegramApiException e) {
                    log.error(e);
                }
            }
            return null;
        }
        return null;
    }

    @Override
    public void onApplicationEvent(SendMessageEvent event) {
        try {
            execute(event.getMessage());
        } catch (TelegramApiException e) {
            log.error(String.format("Can't send message: %s", event.getMessage()), e);
        }
    }
}
