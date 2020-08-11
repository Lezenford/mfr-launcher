package ru.fullrest.mfr.plugins_configuration_utility.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.net.URI

suspend fun <T, V> parallelCalculation(
    items: List<T>,
    action: (T) -> V
): List<V> {
    return items.map { CoroutineScope(Dispatchers.Default).async { action(it) } }
        .map { it.await() }
}

fun RestTemplate.getHeaders(
    url: String,
    headers: Map<String, String> = emptyMap()
): HttpHeaders {
    val httpEntity = HttpHeaders().also {
        it.setAll(headers)
    }.let { HttpEntity<Unit>(it) }
    return exchange(URI.create(url), HttpMethod.HEAD, httpEntity, Unit.javaClass).headers
}