package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.sha256
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.exception.DownloadFileException
import com.lezenford.mfr.launcher.service.provider.KtorProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.random.Random

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class DownloadFileTask(
    private val properties: ApplicationProperties,
    private val ktorProvider: KtorProvider
) : Task<DownloadFileTask.Properties, Unit>() {

    override suspend fun action(params: Properties) {
        updateDescription("Подготовка к скачиванию")

        val totalSize = params.files.size
        val downloaded = AtomicInteger(0)
        val downloadedBytes = AtomicLong(0)
        val filesForDownload = LinkedBlockingQueue<Properties.File>()

        updateDescription("Анализ существующих файлов")

        updateProgress(0)
        coroutineScope {
            val semaphore = Semaphore(10)
            params.files.forEach { file ->
                launch(Dispatchers.IO) {
                    semaphore.withPermit {
                        val existFile = file.mainPath
                        if (!existFile.exists() || !existFile.sha256().contentEquals(file.sha256)) {
                            filesForDownload.add(file)
                        } else {
                            if (params.applyOptionalPath && file.optionalPath != null) {
                                file.optionalPath.parent.toFile().mkdirs()
                                existFile.copyTo(file.optionalPath, true)
                            }
                            val currentValue = downloaded.incrementAndGet()
                            updateProgress(currentValue, totalSize)
                        }
                    }
                }
            }
        }

        val failed = ConcurrentLinkedQueue<Properties.File>()
        coroutineScope {
            repeat(properties.server.connectionCount) {
                launch(Dispatchers.IO) {
                    var fileData = filesForDownload.poll()
                    while (fileData != null) {
                        try {
                            downloadWithRetries(params.host, fileData) { downloadedBytes.addAndGet(it.toLong()) }
                            if (params.applyOptionalPath && fileData.optionalPath != null) {
                                val target = properties.gameFolder.resolve(fileData.optionalPath).also {
                                    it.parent.toFile().mkdirs()
                                }
                                properties.gameFolder.resolve(fileData.mainPath).copyTo(target, true)
                            }
                            val currentValue = downloaded.incrementAndGet()
                            updateProgress(currentValue, totalSize)
                            updateDescription(
                                "Скачано файлов: $currentValue/$totalSize (${formatSize(downloadedBytes.get())})"
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            log.error("Download file error ${fileData.mainPath}, ${fileData.storage}", e)
                            failed.add(fileData)
                        }
                        fileData = filesForDownload.poll()
                    }
                }
            }
        }

        if (failed.isNotEmpty()) {
            log.error("Files didn't download: ${failed.map { it.mainPath }}")
            throw DownloadFileException("Не удалось скачать файлов: ${failed.size}")
        }
        updateProgress(100)
    }

    /**
     * Обрыв и молчание сети повторяются с того места, где остановились; несовпадение
     * контрольной суммы означает битый файл, и следующая попытка начинается с нуля.
     * Ответ сервера с кодом ошибки не повторяется: серверные сбои уже перебрал HttpRequestRetry,
     * а клиентские коды означают расхождение манифеста с хранилищем.
     */
    private suspend fun downloadWithRetries(host: String, file: Properties.File, onChunk: (Int) -> Unit) {
        val mainPathFile = properties.gameFolder.resolve(file.mainPath)
        var attempt = 1
        while (true) {
            try {
                ktorProvider.downloadToFile(host, file.storage, mainPathFile, onChunk)
                if (!mainPathFile.sha256().contentEquals(file.sha256)) {
                    mainPathFile.deleteIfExists()
                    throw IllegalStateException("Download failed. ${file.mainPath} has incorrect checksum")
                }
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: DownloadFileException) {
                throw e
            } catch (e: Exception) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw DownloadFileException("Download failed after $attempt attempts: ${file.mainPath}", e)
                }
                log.warn("Download attempt $attempt failed for ${file.mainPath}: ${e.message}")
                delay(retryDelayMillis(attempt))
                attempt++
            }
        }
    }

    private fun retryDelayMillis(attempt: Int): Long {
        val base = RETRY_BASE_DELAY_MILLIS shl (attempt - 1)
        return base + Random.nextLong(-base / 4, base / 4 + 1)
    }

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1L shl 30 -> String.format("%.1f ГБ", bytes.toDouble() / (1L shl 30))
        bytes >= 1L shl 20 -> String.format("%.0f МБ", bytes.toDouble() / (1L shl 20))
        else -> String.format("%.0f КБ", bytes.toDouble() / (1L shl 10))
    }

    data class Properties(
        val host: String,
        val files: List<File>,
        val applyOptionalPath: Boolean
    ) {
        class File(
            val mainPath: Path,
            val optionalPath: Path?,
            val storage: String,
            val sha256: ByteArray
        )
    }

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val RETRY_BASE_DELAY_MILLIS = 2_000L
        private val log by Logger()
    }
}
