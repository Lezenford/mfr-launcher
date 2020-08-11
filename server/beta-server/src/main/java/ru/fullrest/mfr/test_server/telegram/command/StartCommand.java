package ru.fullrest.mfr.test_server.telegram.command;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.test_server.model.entity.TelegramUser;
import ru.fullrest.mfr.test_server.model.entity.UserRole;
import ru.fullrest.mfr.test_server.model.repository.TelegramUserRepository;

@Log4j2
@Component
public class StartCommand extends BotCommand {
    private final TelegramUserRepository telegramUserRepository;

    @Value("${telegram.bot.default-admin}")
    private String defaultBotAdmin;

    public StartCommand(TelegramUserRepository telegramUserRepository) {
        super("start", "register new user");
        this.telegramUserRepository = telegramUserRepository;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        Integer userId = user.getId();
        if (telegramUserRepository.findById(userId).isEmpty()) {
            TelegramUser telegramUser = new TelegramUser();
            telegramUser.setId(userId);
            telegramUser.setUsername(user.getUserName());
            telegramUser.setRole(UserRole.USER);
            if (userId.toString().equals(defaultBotAdmin)) {
                telegramUser.setRole(UserRole.ADMIN);
            }
            telegramUserRepository.save(telegramUser);
            try {
                absSender.execute(new SendMessage(
                        chat.getId(),
                        "Добро пожаловать в консоль бета сервера проекта MFR. Для продолжения работы необходимо повысить уровень доступа, " +
                                "пожалуйста, обратитесь к администратору"
                ));
            } catch (TelegramApiException e) {
                log.error(e);
            }
        }
    }
}
