package com.lezenford.mfr.common.extensions

import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponents
import org.springframework.web.util.UriComponentsBuilder

data class WebClientSpec internal constructor(
    val uri: UriComponents,
    val type: HttpMethod,
    val contentType: MediaType = MediaType.APPLICATION_JSON
)

fun WebClient.spec(params: WebClientSpec): WebClient.RequestBodySpec {
    val (uri, method, contentType) = params
    return method(method).uri(uri.toUriString()).contentType(contentType)
}

fun WebTestClient.spec(params: WebClientSpec): WebTestClient.RequestBodySpec {
    val (uri, method, contentType) = params
    return method(method).uri(uri.toUri()).contentType(contentType)
}

internal fun String.toUriBuilder(): UriComponentsBuilder = UriComponentsBuilder.newInstance().path(this)
internal fun String.toUri(): UriComponents = UriComponentsBuilder.newInstance().path(this).build()

internal fun UriComponents.withMethod(method: HttpMethod, mediaType: MediaType = MediaType.APPLICATION_JSON) =
    WebClientSpec(this, method, mediaType)