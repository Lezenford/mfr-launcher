package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.extensions.sha256
import com.lezenford.mfr.launcher.exception.TaskExecuteException
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.provider.KtorProvider
import com.lezenford.mfr.launcher.testProperties
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.writeBytes

class DownloadFileTaskTest {

    @TempDir
    lateinit var folder: Path

    private val good = ByteArray(20_000) { it.toByte() }
    private val goodHash: ByteArray = MessageDigest.getInstance("SHA-256").digest(good)

    @BeforeEach
    fun connected() {
        State.serverConnection.value = true
    }

    private fun task(handler: suspend (String) -> ByteArray): DownloadFileTask {
        val client = HttpClient(MockEngine { request ->
            respond(content = ByteReadChannel(handler(request.url.encodedPath)), status = HttpStatusCode.OK)
        }) { install(HttpTimeout) }
        val properties = testProperties(folder)
        return DownloadFileTask(properties, KtorProvider(client, properties))
    }

    private fun file(name: String) = DownloadFileTask.Properties.File(
        mainPath = folder.resolve(name),
        optionalPath = null,
        storage = name,
        sha256 = goodHash
    )

    @Test
    fun `битый ответ не принимается, повтор с нуля даёт файл с верной контрольной суммой`() = runBlocking {
        val attempts = AtomicInteger(0)
        val task = task { if (attempts.incrementAndGet() == 1) ByteArray(20_000) { 7 } else good }

        task.execute(DownloadFileTask.Properties("https://host.example", listOf(file("a.bin")), applyOptionalPath = false))

        assertEquals(2, attempts.get())
        assertArrayEquals(goodHash, folder.resolve("a.bin").sha256())
    }

    @Test
    fun `файл с верной контрольной суммой не скачивается повторно`() = runBlocking {
        folder.resolve("a.bin").writeBytes(good)
        val attempts = AtomicInteger(0)
        val task = task { attempts.incrementAndGet(); good }

        task.execute(DownloadFileTask.Properties("https://host.example", listOf(file("a.bin")), applyOptionalPath = false))

        assertEquals(0, attempts.get())
    }

    @Test
    fun `ошибка сервера роняет задачу, а не помечает файл скачанным`() {
        val client = HttpClient(MockEngine { respond(content = ByteReadChannel.Empty, status = HttpStatusCode.NotFound) }) { install(HttpTimeout) }
        val properties = testProperties(folder)
        val task = DownloadFileTask(properties, KtorProvider(client, properties))

        assertThrows(TaskExecuteException::class.java) {
            runBlocking {
                task.execute(DownloadFileTask.Properties("https://host.example", listOf(file("a.bin")), applyOptionalPath = false))
            }
        }
        assertEquals(false, folder.resolve("a.bin").toFile().exists())
    }
}
