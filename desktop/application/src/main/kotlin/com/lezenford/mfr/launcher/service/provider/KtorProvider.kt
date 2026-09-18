package com.lezenford.mfr.launcher.service.provider

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.exception.DownloadFileException
import com.lezenford.mfr.launcher.exception.DownloadStalledException
import com.lezenford.mfr.launcher.model.dto.Version
import com.lezenford.mfr.launcher.service.Location
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.model.dto.GameSchemaResponse
import com.lezenford.mfr.launcher.model.dto.LauncherSchemaResponse
import com.lezenford.mfr.schema.v1.Schema
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.system.measureTimeMillis

@Component
class KtorProvider(
    private val client: HttpClient,
    private val properties: ApplicationProperties
) {
    internal var stallTimeoutMillis: Long = STALL_TIMEOUT_MILLIS

    private val host: String
        get() = when (State.location.value) {
            Location.RU -> properties.server.ruLocation.address
            Location.EU -> properties.server.euLocation.address
        }.let { "https://$it" }

    suspend fun findActiveGameVersion(): String {
        return client.get("$host/v1/game/version") {
            header(CLIENT_ID_HEADER, State.clientId.value)
            parameter("os", "WINDOWS")
            apiTimeout()
        }.call.response.body<Version>().id
    }

    suspend fun findGameVersionSchema(version: String): GameSchemaResponse {
        return client.get("$host/v1/game/files") {
            header(CLIENT_ID_HEADER, State.clientId.value)
            parameter("os", "WINDOWS")
            parameter("version", version)
            parameter("region", State.location.value)
            apiTimeout()
        }.call.response.body<GameSchemaResponse>()
    }

    suspend fun findGameSchema(host: String, path: String): Schema {
        return Schema.parseFrom(client.get("${host}/${path}") {
            header(CLIENT_ID_HEADER, State.clientId.value)
            manifestTimeout()
        }.call.response.readBytes())
    }

    suspend fun findGameFilesPlan(host: String, path: String): com.lezenford.mfr.version.v1.Version {
        return com.lezenford.mfr.version.v1.Version.parseFrom(client.get("${host}/${path}") {
            header(CLIENT_ID_HEADER, State.clientId.value)
            manifestTimeout()
        }.call.response.readBytes())
    }

    suspend fun findActiveLauncherVersion(): String {
        return client.get("${host}/v2/launcher/version") {
            header(CLIENT_ID_HEADER, State.clientId.value)
            parameter("os", "WINDOWS")
            apiTimeout()
        }.call.response.body<Version>().id
    }

    suspend fun findLauncherVersionSchema(version: String): LauncherSchemaResponse {
        return client.get("$host/v2/launcher/files") {
            header(CLIENT_ID_HEADER, State.clientId.value)
            parameter("os", "WINDOWS")
            parameter("version", version)
            parameter("region", State.location.value)
            apiTimeout()
        }.call.response.body<LauncherSchemaResponse>()
    }

    /**
     * Скачивает объект хранилища в [target], продолжая с того места, на котором оборвалась
     * прошлая попытка. Незавершённые данные живут в соседнем файле с суффиксом [PART_SUFFIX]
     * и переносятся на место одним атомарным ходом, поэтому [target] либо отсутствует, либо
     * содержит ровно то, что отдал сервер. Проверка содержимого остаётся за вызывающим.
     *
     * Объекты в хранилище после публикации не перезаписываются, поэтому склеивать части
     * разных попыток безопасно и сверять ETag не требуется.
     *
     * Тело читается потоком через [HttpStatement.execute]: обычный `get()` сохраняет ответ
     * целиком в памяти, и файл в сотни мегабайт сначала занял бы ОЗУ, а уже потом диск.
     */
    suspend fun downloadToFile(host: String, path: String, target: Path, onChunk: (Int) -> Unit = {}) {
        val part = target.resolveSibling(target.fileName.toString() + PART_SUFFIX)
        Files.createDirectories(target.parent)
        val offset = if (part.exists()) part.fileSize() else 0L
        client.prepareGet("${host.trimEnd('/')}/$path") {
            // Пустой объект на любой диапазон отвечает 416, поэтому без накопленной части
            // запрос идёт обычным GET.
            if (offset > 0) header(HttpHeaders.Range, "bytes=$offset-")
        }.execute { response ->
            when {
                response.status == HttpStatusCode.RequestedRangeNotSatisfiable -> {
                    // Либо объект пустой, либо накопленная часть уже покрывает его целиком.
                }
                response.status == HttpStatusCode.PartialContent -> {
                    copyWithWatchdog(response.bodyAsChannel(), part, append = true, onChunk)
                }
                response.status.isSuccess() -> {
                    if (offset > 0) log.info("Server ignored range request for $path, downloading from scratch")
                    copyWithWatchdog(response.bodyAsChannel(), part, append = false, onChunk)
                }
                else -> throw DownloadFileException("Request $path returned error code ${response.status}")
            }
        }
        if (!part.exists()) {
            Files.createFile(part)
        }
        Files.move(part, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private suspend fun copyWithWatchdog(channel: ByteReadChannel, part: Path, append: Boolean, onChunk: (Int) -> Unit) {
        val options = if (append) {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
        } else {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        }
        try {
            Files.newOutputStream(part, *options).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = withTimeoutOrNull(stallTimeoutMillis) { channel.readAvailable(buffer, 0, buffer.size) }
                        ?: throw DownloadStalledException("No data received for ${stallTimeoutMillis / 1000} seconds")
                    if (read < 0) break
                    if (read > 0) {
                        output.write(buffer, 0, read)
                        onChunk(read)
                    }
                }
            }
        } catch (e: Exception) {
            channel.cancel(e)
            throw e
        }
    }

    suspend fun checkAvailable(host: String): Int? {
        return runCatching {
            measureTimeMillis {
                client.get("$host/ping") {
                    timeout {
                        requestTimeoutMillis = 5_000
                        connectTimeoutMillis = 5_000
                        socketTimeoutMillis = 5_000
                    }
                }.call.takeIf { it.response.status.isSuccess() }
                    ?: throw IllegalArgumentException("Ping failed to $host")
            }.toInt()
        }.getOrNull()
    }

    private fun HttpRequestBuilder.apiTimeout() = timeout { requestTimeoutMillis = API_TIMEOUT_MILLIS }

    private fun HttpRequestBuilder.manifestTimeout() = timeout { requestTimeoutMillis = MANIFEST_TIMEOUT_MILLIS }

    companion object {
        private const val CLIENT_ID_HEADER = "X-Client-ID"
        private const val PART_SUFFIX = ".part"
        private const val BUFFER_SIZE = 256 * 1024
        private const val API_TIMEOUT_MILLIS = 30_000L
        private const val MANIFEST_TIMEOUT_MILLIS = 120_000L
        private const val STALL_TIMEOUT_MILLIS = 60_000L
        private val log by Logger()
    }
}
