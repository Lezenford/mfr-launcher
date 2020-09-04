package ru.fullrest.mfr.server.telegram.callback_module;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.HistoryService;
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
public class StatisticsCallbackModule extends SecureCallbackModule {
    private final TelegramUserService telegramUserService;
    private final HistoryService historyService;

    private final UserRole accessRole = UserRole.ADMIN;
    private final String name = "Statistics";
    private final static String REFRESH = "Refresh";
    private final static String CLOSE = "Close";

    @Override
    protected BotApiMethod<?> init(Chat chat) {
        return new SendMessage().setChatId(chat.getId())
                .setText(getStatistics())
                .setReplyMarkup(new InlineKeyboardMarkup(getButtons()));

    }

    @Override
    protected BotApiMethod<?> process(@NotNull CallbackQuery callbackQuery, @NotNull CallbackData callbackData) {
        switch (callbackData.getData()) {
            case REFRESH: {
                String statistics = getStatistics().strip();
                if (!statistics.equals(callbackQuery.getMessage().getText())) {
                    return new EditMessageText().setChatId(callbackQuery.getMessage().getChatId())
                            .setMessageId(callbackQuery.getMessage().getMessageId())
                            .setReplyMarkup(new InlineKeyboardMarkup(getButtons()))
                            .setText(statistics);
                } else {
                   return new AnswerCallbackQuery().setCallbackQueryId(callbackQuery.getId());
                }
            }
            case CLOSE: {
                return new EditMessageText()
                        .setChatId(callbackQuery.getMessage().getChatId())
                        .setMessageId(callbackQuery.getMessage().getMessageId())
                        .setReplyMarkup(null)
                        .setText("Для просмотра статистики используйте /statistics");
            }
            default: {
                return new AnswerCallbackQuery().setCallbackQueryId(callbackQuery.getId());
            }
        }
    }

    private List<List<InlineKeyboardButton>> getButtons() {
        try {
            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

            buttons.add(Collections.singletonList(new InlineKeyboardButton("Обновить")
                                                          .setCallbackData(CallbackData.convertToJson(name, REFRESH))));
            buttons.add(Collections.singletonList(new InlineKeyboardButton("Закрыть")
                                                          .setCallbackData(CallbackData.convertToJson(name, CLOSE))));
            return buttons;
        } catch (JsonProcessingException | TelegramApiException e) {
            log.error(e);
            return null;
        }
    }

    private String getStatistics() {
        StringBuilder builder = new StringBuilder();
        builder.append("Статистика M[FR]\n")
                .append("Уникальных пользователей: ").append(historyService.getTotalUserCount())
                .append("\n\nСкачивание дистрибутива игры: ")
                .append(historyService.getGameDownloadCount()).append("\n");
        builder.append("\nСкачивание лаунчера игры:\n");
        historyService.getApplicationDownloadCount()
                .forEach(it -> builder.append(it.getVersion())
                        .append(": ").append(it.getCount()).append("\n"));
        builder.append("\nСкачивание обновлений игры:\n");
        historyService.getUpdateDownloadCount()
                .forEach(it -> builder.append(it.getVersion())
                        .append(": ").append(it.getCount()).append("\n"));
        return builder.toString();
    }
}
