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

import java.io.File;
import java.util.concurrent.ConcurrentMap;

@Component
public class SetLauncherFileCommand extends SecureBotCommand {

    private final ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap;
    private final PropertyService propertyService;

    public SetLauncherFileCommand(TelegramUserService telegramUserService, ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap,
            PropertyService propertyService) {
        super("setlauncher", "set game launcher", telegramUserService, UserRole.ADMIN);
        this.callbackAnswerMap = callbackAnswerMap;
        this.propertyService = propertyService;
    }

    @Override
    public void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        callbackAnswerMap.put(chat.getId(), message -> {
            File file = new File(message.getMessage().getText());
            if (file.exists() && !file.isDirectory()) {
                Property property = propertyService.findByType(PropertyType.LAUNCHER);
                if (property == null) {
                    property = new Property();
                    property.setType(PropertyType.LAUNCHER);
                }
                property.setValue(message.getMessage().getText());
                propertyService.save(property);
                absSender.execute(new SendMessage(chat.getId(), "Установлен новый лаунчер: " + message));
            } else {
                absSender.execute(new SendMessage(chat.getId(), "Файл не найден на сервере. Попробуйте еще раз /setlauncher"));
            }
        });
        absSender.execute(new SendMessage(chat.getId(), "Введите название файла"));
    }
}
