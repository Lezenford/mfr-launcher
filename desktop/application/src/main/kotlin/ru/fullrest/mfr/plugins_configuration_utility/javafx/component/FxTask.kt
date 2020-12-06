package ru.fullrest.mfr.plugins_configuration_utility.javafx.component

import kotlinx.coroutines.*
import org.springframework.http.HttpHeaders
import org.springframework.util.StreamUtils
import org.springframework.web.client.ResponseExtractor
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.ProgressBar
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable
import ru.fullrest.mfr.plugins_configuration_utility.service.RestTemplateService
import ru.fullrest.mfr.plugins_configuration_utility.util.Constant
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.coroutines.CoroutineContext

abstract class FxTask<T, K : ProgressBar>(
    protected val restTemplateService: RestTemplateService,
    protected val applicationProperties: ApplicationProperties,
    protected val progressController: K
) : Loggable, CoroutineScope {

    protected abstract suspend fun process(): T

    override val coroutineContext: CoroutineContext = Dispatchers.Default

    suspend fun run(): T = withContext(coroutineContext) { process() }

    suspend fun runAsync(): Deferred<T> = coroutineScope { async { process() } }

    protected fun downloadFile(link: String, params: Map<String, String> = emptyMap()): File {
        val fileName: String = kotlin.run {
            val headers = restTemplateService.getHeaders(link = link, params = params)
            val header = headers[HttpHeaders.CONTENT_DISPOSITION]?.first()
                ?: throw IllegalArgumentException("Header ${HttpHeaders.CONTENT_DISPOSITION} not found")
            header.removeSurrounding("attachment; filename=\"", "\"").takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("Incorrect file name. File name must not be empty")
        }

        val file = File("${applicationProperties.downloadFolder}/$fileName")

        restTemplateService.execute(
            link = link,
            headers = mapOf(DOWNLOAD_FROM_HEADER to file.length().toString()),
            params = params,
            responseExtractor = getDownloadFileExtractor(file)
        )

        return file
    }

    private fun getDownloadFileExtractor(file: File) =
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

    private fun createSpeedJob(file: File) = CoroutineScope(Dispatchers.Default).launch {
        var previousFileSize = file.length()
        val df = DecimalFormat("#0.00", DecimalFormatSymbols(Locale.ENGLISH))
        while (true) {
            val fileSize = file.length()
            val mathContext = MathContext(4, RoundingMode.HALF_EVEN)
            val description = (fileSize - previousFileSize).toBigDecimal().let { b ->
                if (b > CALCULATION_CONSTANT) {
                    val kb = b.divide(CALCULATION_CONSTANT, mathContext)
                    if (kb > CALCULATION_CONSTANT) {
                        val mb = kb.divide(CALCULATION_CONSTANT, mathContext)
                        if (mb > CALCULATION_CONSTANT) {
                            val gb = mb.divide(CALCULATION_CONSTANT, mathContext)
                            "Скачивание: ${df.format(gb)} ГБ/с"
                        } else {
                            "Скачивание: ${df.format(mb)} МБ/с"
                        }
                    } else {
                        "Скачивание: ${df.format(kb)} КБ/с"
                    }
                } else {
                    "Скачивание: ${df.format(b)} Б/с"
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
        while (true) {
            val fileSize = file.length()
            if (fileSize > 0) {
                progressController.updateProgress(fileSize, fileLength)
            }
            delay(100)
        }
    }

    companion object {
        private val CALCULATION_CONSTANT = BigDecimal("1000")
        private const val DOWNLOAD_FROM_HEADER = "Range-From"
    }
}