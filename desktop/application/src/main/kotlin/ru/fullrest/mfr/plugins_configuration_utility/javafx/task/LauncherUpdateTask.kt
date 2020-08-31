package ru.fullrest.mfr.plugins_configuration_utility.javafx.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class LauncherUpdateTask(
    private val propertiesRepository: PropertiesRepository,
    private val applicationProperties: ApplicationProperties,
    private val alertController: AlertController,
    private val restTemplate: RestTemplate
) : FxTask<Boolean>() {

    val interrupted: AtomicBoolean = AtomicBoolean(false)

    override suspend fun process(): Boolean {
        progressController.updateProgress(0, 0)
        progressController.setDescription("Подготовка к скачиванию")

        val betaKey = propertiesRepository.findByKey(PropertyKey.BETA)?.value

        val clientKey = propertiesRepository.findByKey(PropertyKey.INSTANCE_KEY)?.value
            ?: alertController.error(description = "Ошибка запуска приложения")

        val server = betaKey?.let { applicationProperties.testServerLink } ?: applicationProperties.serverLink

        val launcherFileName: String = try {
            val headers = restTemplate.getHeaders(
                url = "$server${Links.LAUNCHER_DOWNLOAD}",
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

        val updaterFileName: String = try {
            val headers = restTemplate.getHeaders(
                url = "$server${Links.LAUNCHER_UPDATER_DOWNLOAD}",
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

        val launcher = File("downloading/$launcherFileName")
        val updater = File("downloading/$updaterFileName")

        try {
            restTemplate.execute(
                "$server${Links.LAUNCHER_DOWNLOAD}",
                HttpMethod.GET,
                RequestCallback { clientHttpRequest ->
                    clientHttpRequest.headers["Range-From"] = "${launcher.length()}"
                    betaKey?.also {
                        clientHttpRequest.headers[HttpHeaders.AUTHORIZATION] = "Bearer $betaKey"
                    }
                    clientHttpRequest.headers[HttpHeaders.COOKIE] = "Key=$clientKey"
                },
                ResponseExtractor { clientHttpResponse ->
                    val fileLength = clientHttpResponse.headers[HttpHeaders.CONTENT_LENGTH]?.first()?.toLong() ?: 0

                    //Update download speed
                    val speedJob = createSpeedJob(launcher)

                    //Update progress
                    val updateProgressJob = updateProgressJob(launcher, fileLength)

                    try {
                        BufferedInputStream(clientHttpResponse.body).use { inputStream ->
                            while (inputStream.available() > 0) {
                                FileOutputStream(launcher, true).use { outputStream ->
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

        try {
            restTemplate.execute(
                "$server${Links.LAUNCHER_UPDATER_DOWNLOAD}",
                HttpMethod.GET,
                RequestCallback { clientHttpRequest ->
                    clientHttpRequest.headers["Range-From"] = "${updater.length()}"
                    betaKey?.also {
                        clientHttpRequest.headers[HttpHeaders.AUTHORIZATION] = "Bearer $betaKey"
                    }
                    clientHttpRequest.headers[HttpHeaders.COOKIE] = "Key=$clientKey"
                },
                ResponseExtractor { clientHttpResponse ->
                    val fileLength = clientHttpResponse.headers[HttpHeaders.CONTENT_LENGTH]?.first()?.toLong() ?: 0

                    //Update download speed
                    val speedJob = createSpeedJob(updater)

                    //Update progress
                    val updateProgressJob = updateProgressJob(updater, fileLength)

                    try {
                        BufferedInputStream(clientHttpResponse.body).use { inputStream ->
                            while (inputStream.available() > 0) {
                                FileOutputStream(updater, true).use { outputStream ->
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

        val file = try {
            val file = File("update")
            Files.move(updater.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            file
        } catch (e: Exception) {
            alertController.error(exception = e)
            return false
        }
        Runtime.getRuntime().exec(
            "\"${File("jdk/bin/java.exe").absoluteFile}\" -jar \"${file.absoluteFile}\"",
            null,
            File("").absoluteFile
        )
        exitProcess(0)
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
                            "Скачивание новой версии лаунчера: ${df.format(gb)} ГБ/с"
                        } else {
                            "Скачивание новой версии лаунчера: ${df.format(mb)} МБ/с"
                        }
                    } else {
                        "Скачивание новой версии лаунчера: ${df.format(kb)} КБ/с"
                    }
                } else {
                    "Скачивание новой версии лаунчера: ${df.format(b)} Б/с"
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