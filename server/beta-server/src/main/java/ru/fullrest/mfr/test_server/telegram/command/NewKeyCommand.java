package ru.fullrest.mfr.test_server.telegram.command;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.test_server.model.entity.AccessKey;
import ru.fullrest.mfr.test_server.model.entity.TelegramUser;
import ru.fullrest.mfr.test_server.model.entity.UserRole;
import ru.fullrest.mfr.test_server.model.repository.AccessKeyRepository;
import ru.fullrest.mfr.test_server.model.repository.TelegramUserRepository;
import ru.fullrest.mfr.test_server.telegram.CallbackAnswer;
import ru.fullrest.mfr.test_server.telegram.TelegramBot;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@Log4j2
@Component
public class NewKeyCommand extends SecureBotCommand {

    private final AccessKeyRepository accessKeyRepository;
    private final ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap;

    public NewKeyCommand(TelegramUserRepository telegramUserRepository,
            AccessKeyRepository accessKeyRepository, ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap) {
        super("newkey", "create a new key", telegramUserRepository, UserRole.ADMIN);
        this.accessKeyRepository = accessKeyRepository;
        this.callbackAnswerMap = callbackAnswerMap;
    }

    @Override
    public void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        callbackAnswerMap.put(chat.getId(), (message) -> {
            AccessKey key = new AccessKey();
            key.setActive(true);
            key.setUsed(false);
            key.setKey(UUID.randomUUID().toString());
            key.setCreateDate(LocalDateTime.now());
            key.setCreatedTelegramUser(telegramUser.getId());
            key.setUser(message);
            accessKeyRepository.save(key);
            absSender.execute(new SendMessage(chat.getId(), "Ключ для пользователя: " + message + "\n" + key.getKey()));
        });
        absSender.execute(new SendMessage(
                chat.getId(),
                "Введите имя пользователя, для которого необходимо сгенерировать ключ"
        ));
    }
}
