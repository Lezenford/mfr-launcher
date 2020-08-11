package ru.fullrest.mfr.server.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.ApiContext;

@Configuration
@RequiredArgsConstructor
public class TelegramConfiguration {

    private final PropertiesConfiguration configuration;

    @Bean
    public DefaultAbsSender telegramBot() {
        return new DefaultAbsSender(ApiContext.getInstance(DefaultBotOptions.class)) {
            @Override
            public String getBotToken() {
                return configuration.getToken();
            }
        };
    }
}
