package ru.fullrest.mfr.server.telegram.command;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.model.repository.TelegramUserRepository;
import ru.fullrest.mfr.server.telegram.TelegramBot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
@Component
public class SetUserRoleCommand extends SecureBotCommand {

    public SetUserRoleCommand(TelegramUserRepository telegramUserRepository) {
        super("setRole", "Set role to user", telegramUserRepository, UserRole.ADMIN);
    }

    @Override
    public void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        telegramUserRepository.findAll().forEach((it) ->
                                                         buttons.add(Collections.singletonList(
                                                                 new InlineKeyboardButton(String.format("%s: %s", it.getUsername(), it.getRole()))
                                                                         .setCallbackData(String.valueOf(it.getId())))));
        buttons.add(Collections.singletonList(new InlineKeyboardButton("Отмена").setCallbackData("cancel")));
        SendMessage sendMessage = new SendMessage(chat.getId(), "Выберите пользователя, которому необходимо изменить роль");
        sendMessage.setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(buttons));
        absSender.execute(sendMessage);
    }
}
