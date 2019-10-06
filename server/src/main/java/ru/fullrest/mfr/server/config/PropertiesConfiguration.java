package ru.fullrest.mfr.server.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class PropertiesConfiguration {

    @Value("${login}")
    private String defaultLogin;

    @Value("${password}")
    private String defaultPassword;

    @Value("${telegram.chat_id}")
    private long chatId;

    @Value("${telegram.bot_token}")
    private String token;
}
