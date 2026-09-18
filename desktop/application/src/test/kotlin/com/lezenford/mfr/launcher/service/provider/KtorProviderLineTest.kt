package com.lezenford.mfr.launcher.service.provider

import com.lezenford.mfr.launcher.testProperties
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class KtorProviderLineTest {

    @TempDir
    lateinit var folder: Path

    private fun provider(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): KtorProvider {
        val client = HttpClient(MockEngine(handler)) {
            install(HttpTimeout)
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpRequestRetry) {
                maxRetries = 5
                retryIf { _, response -> response.status.value >= 500 }
                exponentialDelay()
            }
        }
        return KtorProvider(client, testProperties(folder))
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) = respond(
        content = ByteReadChannel(body),
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

    @Test
    fun `версия линии запрашивается второй ревизией по сегменту пути`() = runBlocking {
        val paths = mutableListOf<String>()
        val provider = provider { request ->
            paths += request.url.encodedPath
            json("""{"id":"1.5.7"}""")
        }

        val result = provider.findActiveGameVersion("1.5")

        assertEquals(LineVersion.Found("1.5.7"), result)
        assertEquals(listOf("/v2/game/1.5/version"), paths)
    }

    @Test
    fun `404 — линии нет, 503 — в линии пока нечего отдать, и 503 не повторяется`() = runBlocking {
        val calls = AtomicInteger(0)
        val notFound = provider { respond(content = ByteReadChannel.Empty, status = HttpStatusCode.NotFound) }
        val nothingYet = provider { calls.incrementAndGet(); respond(content = ByteReadChannel.Empty, status = HttpStatusCode.ServiceUnavailable) }

        assertEquals(LineVersion.NoSuchLine, notFound.findActiveGameVersion("9.9"))
        assertEquals(LineVersion.NothingYet, nothingYet.findActiveGameVersion("2.0"))
        assertEquals(1, calls.get())
    }

    @Test
    fun `перечень линий читается в порядке сервера`() = runBlocking {
        val provider = provider { json("""{"channels":[{"id":"2.0"},{"id":"1.5"}]}""") }

        assertEquals(listOf("2.0", "1.5"), provider.findGameChannels())
    }

    @Test
    fun `сбой сервера на перечне — ошибка, а не пустой перечень`() {
        val provider = provider { respond(content = ByteReadChannel.Empty, status = HttpStatusCode.BadRequest) }

        val error = runCatching { runBlocking { provider.findGameChannels() } }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException, "$error")
    }
}
