package ru.fullrest.mfr.server.telegram.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.Property;
import ru.fullrest.mfr.server.model.entity.PropertyType;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.PropertyService;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.telegram.component.CallbackAnswer;
import ru.fullrest.mfr.server.telegram.TelegramBot;
import ru.fullrest.mfr.server.telegram.component.SecureBotCommand;

import java.util.concurrent.ConcurrentMap;

@Component
public class SetLauncherVersionCommand extends SecureBotCommand {
    private final ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap;
    private final PropertyService propertyService;

    public SetLauncherVersionCommand(TelegramUserService telegramUserService, ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap,
            PropertyService propertyService) {
        super("setlauncherversion", "set game launcher version", telegramUserService, UserRole.ADMIN);
        this.callbackAnswerMap = callbackAnswerMap;
        this.propertyService = propertyService;
    }

    @Override
    public void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        callbackAnswerMap.put(chat.getId(), message -> {
            Property property = propertyService.findByType(PropertyType.LAUNCHER_VERSION);
            if (property == null) {
                property = new Property();
                property.setType(PropertyType.LAUNCHER_VERSION);
            }
            property.setValue(message.getMessage().getText());
            propertyService.save(property);
            absSender.execute(new SendMessage(chat.getId(), "Установлен новая версия лаунчера: " + message));
        });
        absSender.execute(new SendMessage(chat.getId(), "Введите версию ланчера"));
    }
}
