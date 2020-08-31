package ru.fullrest.mfr.server.telegram;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface CallbackAnswer {

    void execute(String message) throws TelegramApiException;
}
