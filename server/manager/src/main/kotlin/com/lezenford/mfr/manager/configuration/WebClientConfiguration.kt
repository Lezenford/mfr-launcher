package com.lezenford.mfr.manager.configuration

import com.lezenford.mfr.manager.configuration.properties.StorageProperties
import com.lezenford.mfr.manager.configuration.properties.TelegramProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
    private val storageProperties: StorageProperties,
    private val telegramProperties: TelegramProperties
) {

    @Bean("storageServiceWebClient")
    fun storageServiceWebClient(): WebClient = WebClient.builder()
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .baseUrl(storageProperties.serverUrl)
        .build()

    @Bean("telegramWebClient")
    fun telegramWebClient(): WebClient = WebClient.builder()
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .baseUrl("https://${telegramProperties.serverAddress}/bot${telegramProperties.token}/")
        .build()
}