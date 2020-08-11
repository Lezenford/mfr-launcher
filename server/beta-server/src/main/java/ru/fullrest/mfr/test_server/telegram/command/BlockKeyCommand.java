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

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

@Log4j2
@Component
public class BlockKeyCommand extends SecureBotCommand {

    private final ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap;
    private final AccessKeyRepository accessKeyRepository;

    public BlockKeyCommand(ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap, AccessKeyRepository accessKeyRepository,
            TelegramUserRepository telegramUserRepository) {
        super("blockkey", "block active key", telegramUserRepository, UserRole.ADMIN);
        this.callbackAnswerMap = callbackAnswerMap;
        this.accessKeyRepository = accessKeyRepository;
    }

    @Override
    public void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        callbackAnswerMap.put(chat.getId(), message -> {
            String result;
            Optional<AccessKey> optional = accessKeyRepository.findByKey(message);
            if (optional.isPresent()) {
                AccessKey key = optional.get();
                key.setActive(false);
                accessKeyRepository.save(key);
                result = "Ключ заблокирован";
            } else {
                result = "Ключ не найден, введите /blockkey для новой попытки";
            }
            try {
                absSender.execute(new SendMessage(chat.getId(), result));
            } catch (TelegramApiException e) {
                log.error(e);
            }
        });
        absSender.execute(new SendMessage(chat.getId(), "Введите ключ, который желаете заблокировать"));
    }
}
