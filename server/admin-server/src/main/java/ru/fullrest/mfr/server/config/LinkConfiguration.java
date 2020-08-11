package ru.fullrest.mfr.server.config;

import lombok.Data;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class LinkConfiguration {

    private final String gameUpdateStoragePath = "/download/updates/";

    private final String applicationStoragePath = "/download/application/";
}