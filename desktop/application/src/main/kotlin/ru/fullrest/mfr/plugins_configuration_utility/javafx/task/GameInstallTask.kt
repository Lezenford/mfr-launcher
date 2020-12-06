package ru.fullrest.mfr.plugins_configuration_utility.javafx.task

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils
import ru.fullrest.mfr.api.Links
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.exception.ApplicationException
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxTask
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.GlobalProgressController
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository
import ru.fullrest.mfr.plugins_configuration_utility.service.RestTemplateService
import ru.fullrest.mfr.plugins_configuration_utility.util.Constant
import java.io.*
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class GameInstallTask(
    private val propertiesRepository: PropertiesRepository,
    restTemplateService: RestTemplateService,
    applicationProperties: ApplicationProperties,
    globalProgressController: GlobalProgressController
) : FxTask<Boolean, GlobalProgressController>(restTemplateService, applicationProperties, globalProgressController) {

    val interrupted: AtomicBoolean = AtomicBoolean(false)
    val installed: AtomicBoolean = AtomicBoolean(false)

    override suspend fun process(): Boolean {
        progressController.updateProgress(0, 0)
        progressController.setDescription("Подготовка к скачиванию")

        val file = downloadFile(Links.GAME_DOWNLOAD)

        progressController.updateProgress(1, 1)
        progressController.setDescription("Загрузка завершена. Подготовка к установке")
        try {
            unzipFiles(file, File("game"))
        } catch (e: IOException) {
            log().error(e)
            throw ApplicationException("Ошибка установки")
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
        propertiesRepository.findByKey(PropertyKey.BETA)?.value?.also { key ->
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
}