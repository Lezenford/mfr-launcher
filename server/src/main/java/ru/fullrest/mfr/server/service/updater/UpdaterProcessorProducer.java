package ru.fullrest.mfr.server.service.updater;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.fullrest.mfr.server.service.updater.event.SendMessageEvent;

import java.util.*;

@Log4j2
@Async("updaterSenderThreadPool")
@Component
@RequiredArgsConstructor
public class UpdaterProcessorProducer {
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final Map<Long, Set<Integer>> CONSUMERS = new HashMap<>();

    public void sendUpdate(Status status, List<List<InlineKeyboardButton>> buttons) {
        CONSUMERS.forEach((chatId, messages) -> {
            messages.forEach(messageId -> {
                final EditMessageText message = new EditMessageText().setChatId(chatId)
                        .setMessageId(messageId)
                        .setText(status.toString())
                        .setReplyMarkup(new InlineKeyboardMarkup(buttons));
                applicationEventPublisher.publishEvent(new SendMessageEvent(this, message));
            });
        });
    }

    public void addConsumer(Long chatId, Integer messageId) {
        CONSUMERS.putIfAbsent(chatId, new HashSet<>());
        CONSUMERS.get(chatId).add(messageId);
    }

    public void removeConsumer(Long chatId, Integer messageId) {
        CONSUMERS.getOrDefault(chatId, Collections.emptySet()).remove(messageId);
        if (CONSUMERS.getOrDefault(chatId, Collections.emptySet()).isEmpty()) {
            CONSUMERS.remove(chatId);
        }
    }
}
