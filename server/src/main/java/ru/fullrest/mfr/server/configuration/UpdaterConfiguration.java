package ru.fullrest.mfr.server.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@EnableAsync
@Configuration
public class UpdaterConfiguration {

    @Bean
    public ExecutorService updaterSenderThreadPool() {
        return Executors.newSingleThreadExecutor();
    }
}
