package com.lezenford.mfr.launcher.config

import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.pipeline.PipelineInterceptor
import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KtorConfiguration {

    @Bean
    fun ktorClient(applicationProperties: ApplicationProperties): HttpClient {
        return HttpClient(Java) {
            engine {
                this.threadsCount = applicationProperties.server.connectionCount
                pipelining = false
                protocolVersion = java.net.http.HttpClient.Version.HTTP_1_1
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 10_000
                requestTimeoutMillis = 600_000
                socketTimeoutMillis = 5_000
            }
            install(HttpRequestRetry) {
                maxRetries = 5
                retryIf { _, response ->
                    response.status.value >= 500
                }
                retryOnExceptionIf { _, _ ->
                    true
                }
                exponentialDelay()
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(UserAgent) {
                agent = "mfr-launcher"
            }
            install(Logging) {
                level = LogLevel.NONE
            }
        }
    }
}