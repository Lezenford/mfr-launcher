package ru.fullrest.mfr.test_server;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.fullrest.mfr.test_server.telegram.TelegramBot;

@SpringBootApplication
@RequiredArgsConstructor
@EnableJpaRepositories
public class TestServerApplication implements CommandLineRunner {

    private final TelegramBot telegramBot;

    public static void main(String[] args) {
        SpringApplication.run(TestServerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
//        telegramBot.setWebhook(telegramBot.getBotPath() + telegramBot.getBotToken(), null);
    }
}

