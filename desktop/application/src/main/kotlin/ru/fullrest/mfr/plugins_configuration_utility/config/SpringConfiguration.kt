package ru.fullrest.mfr.plugins_configuration_utility.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class SpringConfiguration {

    @Bean
    fun restTemplate(): RestTemplate =
        RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
            .build()

    @Bean
    fun objectMapper() = jacksonObjectMapper()
}