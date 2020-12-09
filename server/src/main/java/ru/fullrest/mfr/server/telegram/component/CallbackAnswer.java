package ru.fullrest.mfr.server.telegram.component;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public interface CallbackAnswer {

    void execute(Update message) throws TelegramApiException;
}
