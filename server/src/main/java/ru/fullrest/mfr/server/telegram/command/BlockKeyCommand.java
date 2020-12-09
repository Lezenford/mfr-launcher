package ru.fullrest.mfr.server.telegram.command;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.AccessKey;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.model.repository.AccessKeyRepository;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.telegram.component.CallbackAnswer;
import ru.fullrest.mfr.server.telegram.TelegramBot;
import ru.fullrest.mfr.server.telegram.component.SecureBotCommand;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

@Profile("private")
@Log4j2
@Component
public class BlockKeyCommand extends SecureBotCommand {

    private final ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap;
    private final AccessKeyRepository accessKeyRepository;

    public BlockKeyCommand(ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap, AccessKeyRepository accessKeyRepository,
            TelegramUserService telegramUserService) {
        super("blockkey", telegramUserService, UserRole.ADMIN);
        this.callbackAnswerMap = callbackAnswerMap;
        this.accessKeyRepository = accessKeyRepository;
    }

    @Override
    public void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        callbackAnswerMap.put(chat.getId(), message -> {
            String result;
            Optional<AccessKey> optional = accessKeyRepository.findByKey(message.getMessage().getText());
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
