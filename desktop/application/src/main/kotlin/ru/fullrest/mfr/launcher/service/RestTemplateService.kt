package ru.fullrest.mfr.launcher.service

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import ru.fullrest.mfr.common.IDENTITY_HEADER
import ru.fullrest.mfr.common.api.rest.BuildDto
import ru.fullrest.mfr.common.api.rest.CLIENT_PATH
import ru.fullrest.mfr.common.api.rest.Client
import ru.fullrest.mfr.common.api.rest.Content
import ru.fullrest.mfr.common.api.rest.GAME_PATH
import ru.fullrest.mfr.common.exception.ServerMaintenanceException
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.launcher.component.ApplicationStatus
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.exception.OnlineModException
import java.time.LocalDateTime

@Service
class RestTemplateService(
    private val restTemplate: RestTemplate,
    private val applicationProperties: ApplicationProperties,
    private val applicationStatus: ApplicationStatus,
) {

    fun builds(): List<BuildDto> {
        return if (applicationStatus.onlineMode.value) {
            kotlin.runCatching {
                restTemplate.exchange<List<BuildDto>>(
                    url = "https://${applicationProperties.server.address}/api/v1/$GAME_PATH",
                    method = HttpMethod.GET,
                    requestEntity = requestEntity<Unit>()
                ).takeIf { result ->
                    when {
                        result.statusCode.is2xxSuccessful -> true
                        result.statusCode == HttpStatus.SERVICE_UNAVAILABLE -> throw ServerMaintenanceException()
                        else -> false.also { log.error("Request to server for builds return code: ${result.statusCode}") }
                    }
                }?.body ?: emptyList()
            }.getOrElse { throw OnlineModException(it) }
        } else {
            emptyList()
        }
    }

    fun content(lastUpdateDate: LocalDateTime? = null): Content {
        return if (applicationStatus.onlineMode.value) {
            val uri = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(applicationProperties.server.address)
                .path("/api/v1/$GAME_PATH/${applicationStatus.gameBuildActive.value}")
                .apply {
                    lastUpdateDate?.also { queryParam("lastUpdate", it) }
                }.build()
            kotlin.runCatching {
                restTemplate.exchange<Content>(
                    url = uri.toUriString(),
                    method = HttpMethod.GET,
                    requestEntity = requestEntity<Unit>()
                ).takeIf { result ->
                    when {
                        result.statusCode.is2xxSuccessful -> true
                        result.statusCode == HttpStatus.SERVICE_UNAVAILABLE -> throw ServerMaintenanceException()
                        else -> false.also { log.error("Request to server for builds return code: ${result.statusCode}") }
                    }
                }?.body ?: Content(emptyList())
            }.getOrElse { throw OnlineModException(it) }
        } else {
            Content(emptyList())
        }
    }

    fun client(): Client =
        kotlin.runCatching {
            restTemplate.exchange<List<Client>>(
                url = "https://${applicationProperties.server.address}/api/v1/$CLIENT_PATH",
                method = HttpMethod.GET,
                requestEntity = requestEntity<Unit>()
            ).takeIf { result ->
                when {
                    result.statusCode.is2xxSuccessful -> true
                    result.statusCode == HttpStatus.SERVICE_UNAVAILABLE -> throw ServerMaintenanceException()
                    else -> false.also { log.error("Request to server for client return code: ${result.statusCode}") }
                }
            }?.body ?: emptyList()
        }.getOrElse { throw OnlineModException(it) }.first { it.system == applicationProperties.platform }


    private inline fun <reified T> requestEntity(): HttpEntity<T> =
        HttpEntity<T>(HttpHeaders().apply {
            add(IDENTITY_HEADER, applicationProperties.clientId.toString())
        })


    companion object {
        private val log by Logger()
    }
}