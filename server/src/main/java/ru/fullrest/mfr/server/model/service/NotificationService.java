package ru.fullrest.mfr.server.model.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.config.PropertiesConfiguration;

import java.util.concurrent.ConcurrentLinkedQueue;


@Service
@Log4j2
@RequiredArgsConstructor
public class NotificationService {

    private final PropertiesConfiguration configuration;

    private final DefaultAbsSender telegramBot;

    private ConcurrentLinkedQueue<String> unsentMessages = new ConcurrentLinkedQueue<>();


    public void sendNotification(String text) {
        SendMessage message = new SendMessage();
        message.setChatId(configuration.getChatId());
        message.setText(text);
        try {
            telegramBot.execute(message);
        } catch (TelegramApiException e) {
            log.error(e);
            unsentMessages.offer(text);
        }
    }

    @Scheduled(fixedDelay = 120_000L, initialDelay = 180_000L)
    private void checkUnsentMessages() {
        for (int i = 0; i < unsentMessages.size(); i++) {
            String message = unsentMessages.poll();
            if (message != null) {
                sendNotification(message);
            }
        }
    }
}
