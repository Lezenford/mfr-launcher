package ru.fullrest.mfr.test_server.telegram.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.test_server.model.entity.TelegramUser;
import ru.fullrest.mfr.test_server.model.entity.UserRole;
import ru.fullrest.mfr.test_server.model.repository.TelegramUserRepository;
import ru.fullrest.mfr.test_server.telegram.TelegramBot;

import java.util.Optional;

@Log4j2
@Component
@RequiredArgsConstructor
public class SetUserRoleCallbackCommand {
    private final TelegramUserRepository telegramUserRepository;

    public void execute(TelegramBot absSender, User user, Chat chat, Update update) throws TelegramApiException {
        Optional<TelegramUser> optional = telegramUserRepository.findById(user.getId());
        if (optional.isPresent()) {
            TelegramUser telegramUser = optional.get();
            if (telegramUser.getRole() == UserRole.ADMIN) {
                if (update.hasCallbackQuery()) {
                    String data = update.getCallbackQuery().getData();
                    if (data.equals("cancel")) {
                        absSender.execute(
                                new EditMessageText()
                                        .setChatId(chat.getId())
                                        .setMessageId(update.getCallbackQuery().getMessage().getMessageId())
                                        .setReplyMarkup(null)
                                        .setText("Запрос на изменение прав отменен"));
                    } else {
                        telegramUserRepository.findById(Integer.valueOf(data)).ifPresent(it -> {
                            if (it.getRole() == UserRole.ADMIN) {
                                it.setRole(UserRole.USER);
                            } else {
                                it.setRole(UserRole.ADMIN);
                            }
                            telegramUserRepository.save(it);

                            try {
                                absSender.execute(new EditMessageText()
                                                          .setChatId(chat.getId())
                                                          .setMessageId(update.getCallbackQuery().getMessage().getMessageId())
                                                          .setReplyMarkup(null)
                                                          .setText(
                                                                  String.format(
                                                                          "Пользователю %s установлена группа %s", it.getUsername(), it.getRole())));
                            } catch (TelegramApiException e) {
                                log.error(e);
                            }
                        });
                    }
                }
            } else {
                absSender.execute(new SendMessage(chat.getId(), "У вас недостаточно прав на выполнение этой операции"));
            }
        } else {
            absSender.execute(new SendMessage(chat.getId(), "You are not registered. Press /start"));
        }
    }
}
