package ru.fullrest.mfr.test_server.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.CommandRegistry;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import ru.fullrest.mfr.test_server.telegram.CallbackAnswer;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;

@Configuration
@RequiredArgsConstructor
public class TelegramConfiguration {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Bean
    public CommandRegistry commandRegistry(Collection<BotCommand> commands, BiConsumer<AbsSender, Message> defaultCommand) {
        CommandRegistry commandRegistry = new CommandRegistry(true, botUsername);
        commands.forEach(commandRegistry::register);
        commandRegistry.registerDefaultAction(defaultCommand);
        return commandRegistry;
    }

    @Bean
    public ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap() {
        return new ConcurrentHashMap<>();
    }
}
