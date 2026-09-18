package com.lezenford.mfr.launcher.service.provider

import com.lezenford.mfr.launcher.exception.DownloadStalledException
import com.lezenford.mfr.launcher.testProperties
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.writer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

class KtorProviderDownloadTest {

    @TempDir
    lateinit var folder: Path

    private val content = ByteArray(300_000) { (it % 251).toByte() }

    private fun provider(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): KtorProvider {
        val client = HttpClient(MockEngine(handler)) { install(HttpTimeout) }
        return KtorProvider(client, testProperties(folder))
    }

    private fun MockRequestHandleScope.fullContent(): HttpResponseData = respond(
        content = ByteReadChannel(content),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentLength, content.size.toString())
    )

    @Test
    fun `качает файл целиком и не оставляет временной части`() = runBlocking {
        val target = folder.resolve("data/file.bin")
        val requests = mutableListOf<String?>()
        val provider = provider { request ->
            requests += request.headers[HttpHeaders.Range]
            fullContent()
        }

        provider.downloadToFile("https://host.example/", "object", target)

        assertArrayEquals(content, target.readBytes())
        assertFalse(folder.resolve("data/file.bin.part").exists())
        assertEquals(listOf<String?>(null), requests)
    }

    @Test
    fun `докачивает с места обрыва по Range`() = runBlocking {
        val target = folder.resolve("file.bin")
        val part = folder.resolve("file.bin.part")
        val offset = 100_000
        part.writeBytes(content.copyOfRange(0, offset))
        val ranges = mutableListOf<String?>()
        val provider = provider { request ->
            ranges += request.headers[HttpHeaders.Range]
            respond(
                content = ByteReadChannel(content.copyOfRange(offset, content.size)),
                status = HttpStatusCode.PartialContent,
                headers = headersOf(HttpHeaders.ContentRange, "bytes $offset-${content.size - 1}/${content.size}")
            )
        }

        provider.downloadToFile("https://host.example", "object", target)

        assertEquals(listOf<String?>("bytes=$offset-"), ranges)
        assertArrayEquals(content, target.readBytes())
        assertFalse(part.exists())
    }

    @Test
    fun `сервер проигнорировал Range — часть отбрасывается и файл собирается заново`() = runBlocking {
        val target = folder.resolve("file.bin")
        folder.resolve("file.bin.part").writeBytes(ByteArray(1000) { 1 })
        val provider = provider { fullContent() }

        provider.downloadToFile("https://host.example", "object", target)

        assertArrayEquals(content, target.readBytes())
    }

    @Test
    fun `416 означает, что накопленная часть уже полная`() = runBlocking {
        val target = folder.resolve("file.bin")
        folder.resolve("file.bin.part").writeBytes(content)
        val provider = provider { respond(content = ByteReadChannel.Empty, status = HttpStatusCode.RequestedRangeNotSatisfiable) }

        provider.downloadToFile("https://host.example", "object", target)

        assertArrayEquals(content, target.readBytes())
    }

    @Test
    fun `пустой объект скачивается пустым файлом без Range`() = runBlocking {
        val target = folder.resolve("empty.bin")
        val ranges = mutableListOf<String?>()
        val provider = provider { request ->
            ranges += request.headers[HttpHeaders.Range]
            respond(content = ByteReadChannel.Empty, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentLength, "0"))
        }

        provider.downloadToFile("https://host.example", "object", target)

        assertEquals(0, target.readBytes().size)
        assertEquals(listOf<String?>(null), ranges)
    }

    @Test
    fun `молчание потока дольше сторожа обрывает попытку, принятые байты остаются в части`() = runBlocking {
        val target = folder.resolve("file.bin")
        val head = content.copyOfRange(0, 50_000)
        val provider = provider {
            val channel = writer(Dispatchers.IO) {
                channel.writeFully(head, 0, head.size)
                channel.flush()
                delay(10_000)
            }.channel
            respond(content = channel, status = HttpStatusCode.OK)
        }
        provider.stallTimeoutMillis = 300

        assertThrows(DownloadStalledException::class.java) {
            runBlocking { provider.downloadToFile("https://host.example", "object", target) }
        }

        assertFalse(target.exists())
        assertArrayEquals(head, folder.resolve("file.bin.part").readBytes())
    }
}
