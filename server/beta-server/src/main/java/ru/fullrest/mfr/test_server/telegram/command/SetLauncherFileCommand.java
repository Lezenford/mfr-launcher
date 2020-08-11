package ru.fullrest.mfr.test_server.telegram.command;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.test_server.model.entity.Property;
import ru.fullrest.mfr.test_server.model.entity.PropertyType;
import ru.fullrest.mfr.test_server.model.entity.TelegramUser;
import ru.fullrest.mfr.test_server.model.entity.UserRole;
import ru.fullrest.mfr.test_server.model.repository.PropertyRepository;
import ru.fullrest.mfr.test_server.model.repository.TelegramUserRepository;
import ru.fullrest.mfr.test_server.telegram.CallbackAnswer;
import ru.fullrest.mfr.test_server.telegram.TelegramBot;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

@Component
public class SetLauncherFileCommand extends SecureBotCommand {

    private final ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap;
    private final PropertyRepository propertyRepository;

    public SetLauncherFileCommand(TelegramUserRepository telegramUserRepository, ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap,
            PropertyRepository propertyRepository) {
        super("setlauncher", "set game launcher", telegramUserRepository, UserRole.ADMIN);
        this.callbackAnswerMap = callbackAnswerMap;
        this.propertyRepository = propertyRepository;
    }

    @Override
    public void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        callbackAnswerMap.put(chat.getId(), message -> {
            File file = new File(message);
            if (file.exists() && !file.isDirectory()) {
                Optional<Property> optional = propertyRepository.findByType(PropertyType.LAUNCHER);
                Property property;
                if (optional.isPresent()) {
                    property = optional.get();
                } else {
                    property = new Property();
                    property.setType(PropertyType.LAUNCHER);
                }
                property.setValue(message);
                propertyRepository.save(property);
                absSender.execute(new SendMessage(chat.getId(), "Установлен новый лаунчер: " + message));
            } else {
                absSender.execute(new SendMessage(chat.getId(), "Файл не найден на сервере. Попробуйте еще раз /setlauncher"));
            }
        });
        absSender.execute(new SendMessage(chat.getId(), "Введите название файла"));
    }
}