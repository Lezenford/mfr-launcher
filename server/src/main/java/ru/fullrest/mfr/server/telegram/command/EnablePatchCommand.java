package ru.fullrest.mfr.server.telegram.command;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.service.UpdateService;
import ru.fullrest.mfr.server.telegram.TelegramBot;
import ru.fullrest.mfr.server.telegram.component.CallbackAnswer;
import ru.fullrest.mfr.server.telegram.component.SecureBotCommand;

import java.util.concurrent.ConcurrentMap;

@Log4j2
@Component
public class EnablePatchCommand extends SecureBotCommand {

    private final ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap;
    private final UpdateService updateService;

    public EnablePatchCommand(TelegramUserService telegramUserService, ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap,
            UpdateService updateService) {
        super("enablepatch", telegramUserService, UserRole.ADMIN);
        this.callbackAnswerMap = callbackAnswerMap;
        this.updateService = updateService;
    }

    //TODO добавить проверку на текущее состояние патча
    @Override
    protected void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        callbackAnswerMap.put(chat.getId(), message -> {
            String version = message.getMessage().getText();
            Update update = updateService.findByVersion(version);
            if (update != null) {
                update.setActive(true);
                updateService.save(update);
                absSender.execute(new SendMessage(chat.getId(), String.format("Патч %s включен", version)));
            } else {
                absSender.execute(new SendMessage(chat.getId(), "Патч такой версии не существует. Попробуйте еще раз /enablepatch"));
            }
        });
        absSender.execute(new SendMessage(chat.getId(), "Укажите версию патча, который хотите включить"));
    }
}
