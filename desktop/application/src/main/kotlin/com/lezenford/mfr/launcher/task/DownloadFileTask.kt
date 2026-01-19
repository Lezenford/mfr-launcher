package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.sha256
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.service.provider.KtorProvider
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.copyTo
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.outputStream

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

        val fails = LinkedBlockingQueue<Properties.File>()
        val lastFailsCount = AtomicInteger(0)
        val retryCount = AtomicInteger(3)
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

        do {
            supervisorScope {
                repeat(properties.server.connectionCount) {
                    launch(Dispatchers.IO) {
                        var fileData = filesForDownload.poll()
                        while (fileData != null) {
                            try {
                                val mainPathFile = properties.gameFolder.resolve(fileData.mainPath)
                                    .also { it.parent.takeIf { !it.exists() }?.toFile()?.mkdirs() }.also { it.deleteIfExists() }
                                    .also { it.createFile() }
                                ktorProvider.downloadFile(params.host, fileData.storage).toInputStream()
                                    .copyTo(mainPathFile.outputStream())
                                if (!mainPathFile.sha256().contentEquals(fileData.sha256)) {
                                    throw IllegalArgumentException("Download failed. ${fileData.mainPath} has incorrect checksum")
                                }
                                if (params.applyOptionalPath && fileData.optionalPath != null) {
                                    val target = properties.gameFolder.resolve(fileData.optionalPath).also {
                                        it.parent.toFile().mkdirs()
                                    }
                                    mainPathFile.copyTo(target, true)
                                }
                                val currentValue = downloaded.incrementAndGet()
                                updateProgress(currentValue, totalSize)
                                updateDescription("Скачано файлов: $currentValue/$totalSize")
                            } catch (e: Exception) {
                                log.warn("Download file error ${fileData.mainPath}, ${fileData.storage}", e)
                                fails.add(fileData)
                            }

                            fileData = filesForDownload.poll()
                        }
                    }
                }
            }

            filesForDownload.addAll(fails)
            fails.clear()
            // Допускаем до 3 попыток перегрузить файлы, если при этом число файлов не менялось в циклах
            if (lastFailsCount.compareAndSet(filesForDownload.size, filesForDownload.size)) {
                retryCount.decrementAndGet()
            } else {
                retryCount.set(3)
            }
        } while (filesForDownload.isNotEmpty() && retryCount.get() > 0)
        if (filesForDownload.isNotEmpty()) {
            log.error("Files didn't download: $filesForDownload")
            throw IllegalStateException("Can't download all requested files")
        }
        updateProgress(100)
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
        private val log by Logger()
    }
}