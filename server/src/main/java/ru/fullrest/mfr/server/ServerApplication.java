package ru.fullrest.mfr.server;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.fullrest.mfr.server.telegram.TelegramBot;

@SpringBootApplication
@RequiredArgsConstructor
@EnableJpaRepositories
public class ServerApplication implements CommandLineRunner {

    private final TelegramBot telegramBot;

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        telegramBot.setWebhook(telegramBot.getBotPath() + telegramBot.getBotToken(), null);
    }
}

