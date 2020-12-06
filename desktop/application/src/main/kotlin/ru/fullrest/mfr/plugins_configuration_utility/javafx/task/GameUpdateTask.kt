package ru.fullrest.mfr.plugins_configuration_utility.javafx.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.StreamUtils
import ru.fullrest.mfr.api.GameUpdate
import ru.fullrest.mfr.api.Links
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxTask
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.EmbeddedProgressController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.createProgressWindow
import ru.fullrest.mfr.plugins_configuration_utility.service.RestTemplateService
import ru.fullrest.mfr.plugins_configuration_utility.util.Constant
import ru.fullrest.mfr.plugins_configuration_utility.util.listAllFiles
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class GameUpdateTask(
    private val objectMapper: ObjectMapper,
    private val applicationFiles: ApplicationFiles,
    restTemplateService: RestTemplateService,
    applicationProperties: ApplicationProperties
) : FxTask<Boolean, EmbeddedProgressController>(
    restTemplateService,
    applicationProperties,
    createProgressWindow(Stage.getWindows().find { it.isShowing })
) {

    lateinit var listVersions: List<String>

    override suspend fun process(): Boolean {
        progressController.show()
        var successfully = true
        listVersions.forEach { version ->
            progressController.updateProgress(0, 0)
            progressController.setDescription("Подготовка к скачиванию")

            val file = downloadFile(Links.GAME_UPDATE_DOWNLOAD, mapOf("version" to version))

            progressController.updateProgress(1, 1)
            progressController.setDescription("Загрузка завершена. Подготовка к установке")

            val tempDirectory = withContext(Dispatchers.IO) {
                Files.createTempDirectory("mfr_").toFile().also {
                    it.deleteOnExit()
                }
            }

            unzipFiles(file, tempDirectory)

            val plan: GameUpdate = withContext(Dispatchers.IO) {
                val updateInfoFile = tempDirectory.listFiles()?.find { it.name == GameUpdate.FILE_NAME }
                    ?: kotlin.run {
                        progressController.updateProgress(0, 0)
                        progressController.setDescription("Ошибка установки обновления")
                        return@withContext null
                    }
                val json = FileReader(updateInfoFile).use {
                    it.readLines().fold("") { acc, s -> acc + "\n" + s }
                }
                objectMapper.readValue<GameUpdate>(json)
            } ?: kotlin.run {
                successfully = false
                return@forEach
            }

            withContext(Dispatchers.IO) {
                applyUpdate(plan, tempDirectory)
            }
        }
        progressController.setCloseButtonVisible(true)
        return successfully
    }

    private fun unzipFiles(archive: File, destination: File) {
        val count = ZipFile(archive).size()
        progressController.setDescription("Распаковка обновления")
        progressController.updateProgress(0, 0)
        ZipInputStream(FileInputStream(archive), Charset.defaultCharset()).use { inputStream ->
            var unzipFiles = 0
            var entry: ZipEntry? = inputStream.nextEntry
            while (entry != null) {
                val fileName = entry.name
                val file = File(destination.absolutePath + File.separator + fileName)
                if (file.parentFile.exists().not()) {
                    file.parentFile.mkdirs()
                }
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
                progressController.updateProgress(++unzipFiles, count)
            }
        }
        progressController.updateProgress(1, 1)
        progressController.setDescription("Распаковка успешно завершена")
        archive.deleteRecursively()
    }

    private fun applyUpdate(plan: GameUpdate, tempDirectory: File): Boolean {
        val backupDirectory = Files.createTempDirectory("mfr_backup_").toFile().also {
            it.deleteOnExit()
        }

        val fileCounts = plan.addFiles.size + plan.moveFiles.size + plan.removeFiles.size
        var fileCompliteCount = 0
        progressController.updateProgress(0, 0)
        progressController.setDescription("Установка обновления ${plan.version}")
        try {
            plan.removeFiles.forEach {
                val fileForRemove = File("${applicationFiles.gameFolder}${File.separator}$it")
                val backupFile = File("$backupDirectory${File.separator}$it")
                if (fileForRemove.exists()) {
                    backupFile.parentFile.mkdirs()
                    Files.move(fileForRemove.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                progressController.updateProgress(++fileCompliteCount, fileCounts)
            }

            plan.addFiles.forEach {
                val source = File("${tempDirectory}${File.separator}patch${File.separator}$it")
                val backupFile = File("$backupDirectory${File.separator}$it")
                val target = File("${applicationFiles.gameFolder}${File.separator}$it")
                if (target.exists()) {
                    backupFile.parentFile.mkdirs()
                    Files.move(target.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                target.parentFile.mkdirs()
                Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                progressController.updateProgress(++fileCompliteCount, fileCounts)
            }

            plan.moveFiles.forEach {
                val source = File("${applicationFiles.gameFolder}${File.separator}${it.from}")
                val target = File("${applicationFiles.gameFolder}${File.separator}${it.to}")
                if (source.exists()) {
                    Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                progressController.updateProgress(++fileCompliteCount, fileCounts)
            }
            progressController.updateProgress(1, 1)
            progressController.setDescription("Обновление успешно установлено")
            return true
        } catch (e: Exception) {
            log().error(e)
            progressController.setDescription("Ошибка обновления, откат изменений")

            backupDirectory.listAllFiles().forEach { backupFile ->
                val target =
                    File("${applicationFiles.gameFolder}${backupFile.absolutePath.removePrefix(backupDirectory.absolutePath)}")
                Files.move(backupFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                progressController.updateProgress(fileCompliteCount--, fileCounts)
            }
            plan.moveFiles.forEach {
                val source = File("${applicationFiles.gameFolder}${File.separator}${it.to}")
                val target = File("${applicationFiles.gameFolder}${File.separator}${it.from}")
                if (source.exists()) {
                    Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                progressController.updateProgress(--fileCompliteCount, fileCounts)
            }
            progressController.updateProgress(0, 0)
            progressController.setDescription("Ошибка установки обновления ${plan.version}")
            return false
        } finally {
            tempDirectory.deleteRecursively()
            backupDirectory.deleteRecursively()
        }
    }
}