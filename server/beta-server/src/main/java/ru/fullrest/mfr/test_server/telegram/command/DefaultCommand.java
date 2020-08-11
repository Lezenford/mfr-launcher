package ru.fullrest.mfr.test_server.telegram.command;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
public class DefaultCommand implements BiConsumer<AbsSender, Message> {

    @SneakyThrows
    @Override
    public void accept(AbsSender absSender, Message message) {
        absSender.execute(new SendMessage(message.getChatId(), "Unknown command. Please use /help"));
    }
}
