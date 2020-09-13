package ru.fullrest.mfr.plugins_configuration_utility.service

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RequestCallback
import org.springframework.web.client.ResponseExtractor
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.exception.ApplicationException
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository

@Service
class RestTemplateService(
    private val restTemplate: RestTemplate,
    private val propertiesRepository: PropertiesRepository,
    private val applicationProperties: ApplicationProperties
) : Loggable {

    fun getHeaders(link: String, params: Map<String, String> = emptyMap()): HttpHeaders {
        val uri = UriComponentsBuilder.fromHttpUrl("${getServer()}$link").also {
            params.forEach { (param, value) -> it.queryParam(param, value) }
        }.build().toUri()
        return restTemplate.exchange(
            uri,
            HttpMethod.HEAD,
            HttpEntity<Unit>(getHttpHeaders()),
            Unit.javaClass
        ).headers
    }

    fun <T> exchange(
        link: String,
        method: HttpMethod = HttpMethod.GET,
        params: Map<String, String> = emptyMap(),
        clazz: Class<T>
    ): T? {
        val uri = UriComponentsBuilder.fromHttpUrl("${getServer()}$link").also {
            params.forEach { (param, value) -> it.queryParam(param, value) }
        }.build().toUri()
        return restTemplate.exchange(
            uri,
            HttpMethod.GET,
            HttpEntity<Unit>(getHttpHeaders()),
            clazz
        ).body
    }

    fun <T> execute(
        link: String,
        method: HttpMethod = HttpMethod.GET,
        headers: Map<String, String> = emptyMap(),
        params: Map<String, String> = emptyMap(),
        responseExtractor: ResponseExtractor<T>
    ): T? {
        val uri = UriComponentsBuilder.fromHttpUrl("${getServer()}$link").also {
            params.forEach { (param, value) -> it.queryParam(param, value) }
        }.build().toUri()
        return restTemplate.execute(
            uri,
            HttpMethod.GET,
            RequestCallback { clientHttpRequest ->
                clientHttpRequest.headers.putAll(getHttpHeaders())
                headers.forEach { (header, value) ->
                    clientHttpRequest.headers[header] = value
                }
            },
            responseExtractor
        )
    }

    private fun getServer(): String {
        return if (propertiesRepository.existsByKey(PropertyKey.BETA)) {
            applicationProperties.testServerLink
        } else {
            applicationProperties.serverLink
        }
    }

    private fun getHttpHeaders(): HttpHeaders {
        val betaKey = propertiesRepository.findByKey(PropertyKey.BETA)?.value
        val clientKey = propertiesRepository.findByKey(PropertyKey.INSTANCE_KEY)?.value
            ?: throw ApplicationException("Client key doesn't exist")
        return HttpHeaders().also { headers ->
            betaKey?.also { headers.set(HttpHeaders.AUTHORIZATION, "Bearer $betaKey") }
            headers.set(HttpHeaders.COOKIE, "Key=$clientKey")
        }
    }
}