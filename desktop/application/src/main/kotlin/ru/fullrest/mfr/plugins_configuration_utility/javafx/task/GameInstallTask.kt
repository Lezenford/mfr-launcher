package ru.fullrest.mfr.plugins_configuration_utility.javafx.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
//import kotlinx.coroutines.*
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils
import org.springframework.web.client.RequestCallback
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.ResponseExtractor
import org.springframework.web.client.RestTemplate
import ru.fullrest.mfr.api.Links
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxTask
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.AlertController
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository
import ru.fullrest.mfr.plugins_configuration_utility.util.Constant
import ru.fullrest.mfr.plugins_configuration_utility.util.getHeaders
import java.io.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.nio.charset.Charset
import java.nio.file.Files
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class GameInstallTask(
    private val restTemplate: RestTemplate,
    private val propertiesRepository: PropertiesRepository,
    private val applicationProperties: ApplicationProperties,
    private val alertController: AlertController
) : FxTask<Boolean>() {

    val interrupted: AtomicBoolean = AtomicBoolean(false)
    val installed: AtomicBoolean = AtomicBoolean(false)

    override suspend fun process(): Boolean {
        progressController.updateProgress(0, 0)
        progressController.setDescription("Подготовка к скачиванию")

        val betaKey = propertiesRepository.findByKey(PropertyKey.BETA)?.value

        val clientKey = propertiesRepository.findByKey(PropertyKey.INSTANCE_KEY)?.value
            ?: alertController.error(description = "Ошибка запуска приложения")

        val server = betaKey?.let { applicationProperties.testServerLink } ?: applicationProperties.serverLink

        val fileName: String = try {
            val headers = restTemplate.getHeaders(
                url = "$server${Links.GAME_DOWNLOAD}",
                headers = betaKey?.let {
                    mapOf(
                        HttpHeaders.AUTHORIZATION to "Bearer $it",
                        HttpHeaders.COOKIE to "Key=$clientKey"
                    )
                } ?: mapOf(HttpHeaders.COOKIE to "Key=$clientKey")
            )
            val header = headers[HttpHeaders.CONTENT_DISPOSITION]?.first()
                ?: throw IllegalArgumentException("Header ${HttpHeaders.CONTENT_DISPOSITION} not found")
            header.removeSurrounding("attachment; filename=\"", "\"").takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Incorrect file name. File name must not be empty")
        } catch (e: Exception) {
            alertController.error(exception = e)
            return false
        }

        val file = File("downloading/$fileName")

        try {
            restTemplate.execute(
                "$server${Links.GAME_DOWNLOAD}",
                HttpMethod.GET,
                RequestCallback { clientHttpRequest ->
                    clientHttpRequest.headers["Range-From"] = "${file.length()}"
                    betaKey?.also {
                        clientHttpRequest.headers[HttpHeaders.AUTHORIZATION] = "Bearer $betaKey"
                    }
                    clientHttpRequest.headers[HttpHeaders.COOKIE] = "Key=$clientKey"
                },
                ResponseExtractor { clientHttpResponse ->
                    val fileLength = clientHttpResponse.headers[HttpHeaders.CONTENT_LENGTH]?.first()?.toLong() ?: 0

                    //Update download speed
                    val speedJob = createSpeedJob(file)

                    //Update progress
                    val updateProgressJob = updateProgressJob(file, fileLength)

                    try {
                        BufferedInputStream(clientHttpResponse.body).use { inputStream ->
                            while (inputStream.available() > 0) {
                                FileOutputStream(file, true).use { outputStream ->
                                    StreamUtils.copy(inputStream.readNBytes(Constant.writeBufferSize), outputStream)
                                }
                            }
                        }
                    } finally {
                        updateProgressJob.cancel()
                        speedJob.cancel()
                    }
                }
            )
        } catch (e: ResourceAccessException) {
            alertController.error(exception = e)
            return false
        }

        progressController.updateProgress(1, 1)
        progressController.setDescription("Загрузка завершена. Подготовка к установке")
        try {
            unzipFiles(file, File("game"))
        } catch (e: IOException) {
            alertController.error(exception = e, description = "Ошибка установки")
            return false
        }
        copyBetaKey()
        progressController.setDescription("Установка завершена")
        return true
    }

    private fun unzipFiles(archive: File, destination: File) {
        val count = ZipFile(archive).size()
        progressController.setDescription("Установка игры")
        progressController.updateProgress(0, 0)
        ZipInputStream(FileInputStream(archive), Charset.defaultCharset()).use { inputStream ->
            var unzipFiles = 0
            var entry: ZipEntry? = inputStream.nextEntry
            while (entry != null && interrupted.get().not()) {
                val fileName = entry.name
                val file = File(destination.absolutePath + File.separator + fileName)
                if (entry.isDirectory) {
                    if (!file.mkdirs() && !file.exists()) {
                        log().error("Can't create folder! " + file.absolutePath)
                    }
                } else {
                    try {
                        FileOutputStream(file).use { outputStream ->
                            while (inputStream.available() > 0) {
                                StreamUtils.copy(inputStream.readNBytes(Constant.writeBufferSize), outputStream)
                            }
                        }
                        Files.setLastModifiedTime(file.toPath(), entry.lastModifiedTime)
                    } catch (e: Exception) {
                        log().error(e.message)
                    }
                }
                entry = inputStream.nextEntry
                unzipFiles++
                progressController.updateProgress(unzipFiles, count)
            }
        }
        progressController.updateProgress(1, 1)
        progressController.setDescription("Игра успешно установлена")
        installed.set(true)
        Files.deleteIfExists(archive.toPath())
    }

    private fun copyBetaKey() {
        propertiesRepository.findByKey(PropertyKey.BETA)?.value?.also {key ->
            val file = File("game/lua.bin").also {
                if (it.exists().not()) {
                    it.createNewFile()
                }
            }
            FileWriter(file).use { writer ->
                writer.write(key)
            }
        }
    }

    private fun createSpeedJob(file: File) = CoroutineScope(Dispatchers.Default).launch {
        var previousFileSize = file.length()
        val df = DecimalFormat("#0.00", DecimalFormatSymbols(Locale.ENGLISH))
        while (interrupted.get().not()) {
            val fileSize = file.length()
            val mathContext = MathContext(4, RoundingMode.HALF_EVEN)
            val description = (fileSize - previousFileSize).toBigDecimal().let { b ->
                if (b > CALCULATION_CONSTANT) {
                    val kb = b.divide(CALCULATION_CONSTANT, mathContext)
                    if (kb > CALCULATION_CONSTANT) {
                        val mb = kb.divide(CALCULATION_CONSTANT, mathContext)
                        if (mb > CALCULATION_CONSTANT) {
                            val gb = mb.divide(CALCULATION_CONSTANT, mathContext)
                            "Скачивание игры: ${df.format(gb)} ГБ/с"
                        } else {
                            "Скачивание игры: ${df.format(mb)} МБ/с"
                        }
                    } else {
                        "Скачивание игры: ${df.format(kb)} КБ/с"
                    }
                } else {
                    "Скачивание игры: ${df.format(b)} Б/с"
                }
            }
            if (fileSize > 0) {
                progressController.setDescription(description)
                delay(1000)
            } else {
                delay(100)
            }
            previousFileSize = fileSize
        }
    }

    private fun updateProgressJob(file: File, fileLength: Long) = CoroutineScope(Dispatchers.Default).launch {
        while (interrupted.get().not()) {
            val fileSize = file.length()
            if (fileSize > 0) {
                progressController.updateProgress(fileSize, fileLength)
            }
            delay(100)
        }
    }

    companion object {
        private val CALCULATION_CONSTANT = BigDecimal("1000")
    }
}