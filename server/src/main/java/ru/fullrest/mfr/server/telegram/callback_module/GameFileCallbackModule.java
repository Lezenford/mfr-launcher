package ru.fullrest.mfr.server.telegram.callback_module;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
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
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.service.updater.Status;
import ru.fullrest.mfr.server.service.updater.event.OperationUpdateEvent;
import ru.fullrest.mfr.server.service.updater.UpdaterProcessorExecutor;
import ru.fullrest.mfr.server.service.updater.UpdaterProcessorProducer;
import ru.fullrest.mfr.server.telegram.component.CallbackAnswer;
import ru.fullrest.mfr.server.telegram.component.CallbackData;
import ru.fullrest.mfr.server.telegram.component.SecureCallbackModule;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Log4j2
@Getter
@Component
@RequiredArgsConstructor
public class GameFileCallbackModule extends SecureCallbackModule implements ApplicationListener<OperationUpdateEvent> {
    private final TelegramUserService telegramUserService;
    private final UpdaterProcessorExecutor updaterProcessorExecutor;
    private final UpdaterProcessorProducer updaterProcessorProducer;
    private final ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final String name = "File";
    private final UserRole accessRole = UserRole.ADMIN;

    private final static String REFRESH = "Refresh";
    private final static String CLOSE = "Close";
    private final static String CLONE = "Clone";
    private final static String UPDATE = "Update";
    private final static String PATCH = "Patch";
    private final static String CREATE = "Create";
    private final static String CHANGE = "Change";

    @Override
    protected BotApiMethod<?> init(Chat chat) {
        Status status = updaterProcessorExecutor.getStatus();
        return new SendMessage()
                .setChatId(chat.getId())
                .setText(status.toString())
                .setReplyMarkup(new InlineKeyboardMarkup(getButtons(status)));
    }

    @Override
    protected BotApiMethod<?> process(@NotNull CallbackQuery callbackQuery, @NotNull CallbackData callbackData) {
        switch (callbackData.getEvent()) {
            case CLONE: {
                updaterProcessorExecutor.cloneRepository();
                return updateMessage(callbackQuery);
            }
            case UPDATE: {
                updaterProcessorExecutor.updateRepository();
                return updateMessage(callbackQuery);
            }
            case PATCH: {
                updaterProcessorExecutor.prepareUpdate();
                return updateMessage(callbackQuery);
            }
            case CREATE: {
                updaterProcessorExecutor.createUpdate();
                return updateMessage(callbackQuery);
            }
            case REFRESH: {
                return updateMessage(callbackQuery);
            }
            case CHANGE: {
                callbackAnswerMap.put(
                        callbackQuery.getMessage().getChatId(),
                        message -> updaterProcessorExecutor.setChangeLog(message.getMessage().getText())
                                     );
                return new SendMessage().setChatId(callbackQuery.getMessage().getChatId()).setText("Введите историю изменений:");
            }
            case CLOSE: {
                updaterProcessorProducer.removeConsumer(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
                return new EditMessageText()
                        .setChatId(callbackQuery.getMessage().getChatId())
                        .setMessageId(callbackQuery.getMessage().getMessageId())
                        .setReplyMarkup(null)
                        .setText("Для управления файлами сервера используйте  /files");
            }
            default: {
                return new AnswerCallbackQuery().setCallbackQueryId(callbackQuery.getId());
            }
        }
    }

    private BotApiMethod<?> updateMessage(CallbackQuery callbackQuery) {
        updaterProcessorProducer.addConsumer(callbackQuery.getMessage().getChatId(), callbackQuery.getMessage().getMessageId());
        updateMessage();
        return new AnswerCallbackQuery().setCallbackQueryId(callbackQuery.getId());
    }

    private void updateMessage() {
        Status status = updaterProcessorExecutor.getStatus();
        updaterProcessorProducer.sendUpdate(status, getButtons(status));
    }

    private List<List<InlineKeyboardButton>> getButtons(Status status) {
        try {
            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

            if (!status.isRunning()) {
                if (updaterProcessorExecutor.repositoryExist()) {
                    buttons.add(Collections.singletonList(new InlineKeyboardButton("Обновить дистрибутив")
                                                                  .setCallbackData(CallbackData.convertToJson(name, UPDATE))));
                    buttons.add(Collections.singletonList(new InlineKeyboardButton("Подготовить обновление")
                                                                  .setCallbackData(CallbackData.convertToJson(name, PATCH))));
                    buttons.add(Collections.singletonList(new InlineKeyboardButton("Установить историю изменений")
                                                                  .setCallbackData(CallbackData.convertToJson(name, CHANGE))));
                    if (!status.getUpdate().getVersion().isBlank() && !status.getUpdate().getChangeLog().isEmpty()) {
                        buttons.add(Collections.singletonList(new InlineKeyboardButton("Создать обновление")
                                                                      .setCallbackData(CallbackData.convertToJson(name, CREATE))));
                    }
                } else {
                    buttons.add(Collections.singletonList(new InlineKeyboardButton("Скачать дистрибутив")
                                                                  .setCallbackData(CallbackData.convertToJson(name, CLONE))));
                }
            }
            buttons.add(Collections.singletonList(new InlineKeyboardButton("Обновить")
                                                          .setCallbackData(
                                                                  CallbackData.convertToJson(name, REFRESH, String.valueOf(new Date().getTime())))));
            buttons.add(Collections.singletonList(new InlineKeyboardButton("Закрыть")
                                                          .setCallbackData(CallbackData.convertToJson(name, CLOSE))));
            return buttons;
        } catch (JsonProcessingException | TelegramApiException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public void onApplicationEvent(OperationUpdateEvent event) {
        updateMessage();
    }
}
