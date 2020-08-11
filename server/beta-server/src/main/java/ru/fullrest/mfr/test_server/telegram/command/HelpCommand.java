package ru.fullrest.mfr.test_server.telegram.command;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Log4j2
@Component
public class HelpCommand extends BotCommand {
    public HelpCommand() {
        super("help", "list of commands");
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        try {
            absSender.execute(new SendMessage(chat.getId(), "Список команд:"));
        } catch (TelegramApiException e) {
            log.error(e);
        }
    }
}
