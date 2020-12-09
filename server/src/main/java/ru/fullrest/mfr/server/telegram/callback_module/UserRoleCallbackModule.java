package ru.fullrest.mfr.server.telegram.callback_module;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.telegram.component.CallbackData;
import ru.fullrest.mfr.server.telegram.component.SecureCallbackModule;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Log4j2
@Getter
@Component
@RequiredArgsConstructor
public class UserRoleCallbackModule extends SecureCallbackModule {
    private final TelegramUserService telegramUserService;

    private final String name = "Role";
    private final UserRole accessRole = UserRole.ADMIN;

    private final static String CANCEL = "Cancel";

    @Override
    protected BotApiMethod<?> init(Chat chat) {
        try {
            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
            Iterable<TelegramUser> users = telegramUserService.findAll();
            for (TelegramUser user : users) {
                buttons.add(Collections.singletonList(
                        new InlineKeyboardButton(String.format("%s: %s", user.getUsername(), user.getRole()))
                                .setCallbackData(CallbackData.convertToJson(name, String.valueOf(user.getId())))));
            }
            buttons.add(Collections.singletonList(new InlineKeyboardButton("Отмена").setCallbackData(CallbackData.convertToJson(name, CANCEL))));
            SendMessage message = new SendMessage(chat.getId(), "Выберите пользователя, которому необходимо изменить роль");
            message.setReplyMarkup(new InlineKeyboardMarkup().setKeyboard(buttons));
            return message;
        } catch (TelegramApiException | JsonProcessingException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    protected BotApiMethod<?> process(@NotNull CallbackQuery callbackQuery, @NotNull CallbackData callbackData) {
        if (CANCEL.equals(callbackData.getEvent())) {
            return new EditMessageText()
                    .setChatId(callbackQuery.getMessage().getChatId())
                    .setMessageId(callbackQuery.getMessage().getMessageId())
                    .setReplyMarkup(null)
                    .setText("Запрос на изменение прав отменен");
        }
        TelegramUser user = telegramUserService.findById(Integer.valueOf(callbackData.getEvent()));
        String text = "Пользователь не найден";
        if (user != null) {
            if (user.getRole() == UserRole.ADMIN) {
                user.setRole(UserRole.USER);
            } else {
                user.setRole(UserRole.ADMIN);
            }
            telegramUserService.save(user);
            text = String.format("Пользователю %s установлена группа %s", user.getUsername(), user.getRole());

        }
        return new EditMessageText()
                .setChatId(callbackQuery.getMessage().getChatId())
                .setMessageId(callbackQuery.getMessage().getMessageId())
                .setReplyMarkup(null)
                .setText(text);
    }
}
