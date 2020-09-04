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
import ru.fullrest.mfr.server.telegram.TelegramBot;
import ru.fullrest.mfr.server.telegram.component.SecureBotCommand;

@Profile("private")
@Log4j2
@Component
public class ListKeyCommand extends SecureBotCommand {

    private final AccessKeyRepository accessKeyRepository;

    public ListKeyCommand(AccessKeyRepository accessKeyRepository, TelegramUserService telegramUserService) {
        super("listkeys", "list of keys", telegramUserService, UserRole.ADMIN);
        this.accessKeyRepository = accessKeyRepository;
    }

    @Override
    public void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        Iterable<AccessKey> keys = accessKeyRepository.findAll();
        StringBuilder result = new StringBuilder();
        keys.forEach(key -> result.append("User: ")
                .append(key.getUser())
                .append(", Ключ: ")
                .append(key.getKey())
                .append(key.isUsed() ? ", Активирован" : ", Не активирован")
                .append(key.isActive() ? ", Используется" : ", Заблокирован")
                .append("\n"));
        if (result.toString().isBlank()) {
            result.append("Нет выданных ключей");
        }
        absSender.execute(new SendMessage(chat.getId(), result.toString()));
    }
}
