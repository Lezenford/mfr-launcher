package com.lezenford.mfr.launcher.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.StdDateFormat
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lezenford.mfr.common.IDENTITY_HEADER
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.util.MimeTypeUtils
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.netty.http.client.HttpClient
import java.time.Duration

@Configuration
class SpringConfiguration {

    @Bean
    fun webClient(properties: ApplicationProperties): WebClient {
        val strategies = ExchangeStrategies.builder().codecs { codecs ->
            codecs.defaultCodecs().maxInMemorySize(SIZE)
        }.build()
        val serverUri =
            UriComponentsBuilder.newInstance().scheme("https").host(properties.server.address).build().toUriString()
        val client: HttpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(10))
        return WebClient.builder().clientConnector(ReactorClientHttpConnector(client)).baseUrl(serverUri)
            .defaultHeader(IDENTITY_HEADER, properties.clientId.toString())
            .exchangeStrategies(strategies).build()
    }

    @Bean
    fun rSocketClient(properties: ApplicationProperties, objectMapper: ObjectMapper): RSocketRequester {
        val serverUri =
            UriComponentsBuilder.newInstance().scheme("wss").host(properties.server.address).path("api/v2").build()
                .toUri()
        return RSocketRequester.builder()
            .rsocketStrategies {
                it.decoder(Jackson2JsonDecoder())
                it.encoder(Jackson2JsonEncoder())
            }
            .dataMimeType(MimeTypeUtils.APPLICATION_JSON)
            .websocket(serverUri)
    }


    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .setDateFormat(StdDateFormat())
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)

    companion object {
        private const val SIZE = 32 * 1024 * 1024
    }
}