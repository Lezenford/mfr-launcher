package ru.fullrest.mfr.launcher.javafx.task

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.toPath
import ru.fullrest.mfr.javafx.component.ProgressBar
import ru.fullrest.mfr.launcher.component.ApplicationStatus
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.exception.NotEnoughSpaceException
import ru.fullrest.mfr.launcher.javafx.TaskFactory
import ru.fullrest.mfr.launcher.model.entity.Properties
import ru.fullrest.mfr.launcher.service.PropertiesService
import ru.fullrest.mfr.launcher.service.SectionService
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.moveTo

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class GameUpdateTask(
    private val applicationProperties: ApplicationProperties,
    private val sectionService: SectionService,
    private val taskFactory: TaskFactory,
    private val propertiesService: PropertiesService,
    private val applicationStatus: ApplicationStatus,
    private val objectMapper: ObjectMapper
) {

    suspend fun execute(progressBar: ProgressBar) {
        progressBar.updateProgress(0)
        progressBar.updateDescription("Подготовка к обновлению")
        val startTime = objectMapper.writeValueAsString(LocalDateTime.now(ZoneOffset.UTC))
        val files = taskFactory.checkGameUpdateTask().execute().filter {
            it.active.apply {
                if (not()) {
                    applicationProperties.gameFolder.resolve(it.path.toPath()).deleteIfExists()
                }
            }
        }.associateBy { it.id }.toMutableMap()
        if (files.isNotEmpty()) {
            val freeSpace = (applicationProperties.gameFolder.root.toFile().usableSpace / 1024.0).toLong()
            val totalSpace = (files.values.sumOf { it.size } * 1.1 / 1024.0).toLong()
            if (freeSpace < totalSpace) {
                throw NotEnoughSpaceException("Для успешной установки необходимо минимум $totalSpace КБ свободного места. Доступно всего $freeSpace КБ. Установка будет прервана")
            }
            val backup = mutableListOf<Pair<Path, Path>>()
            try {
                files.forEach { (_, value) ->
                    applicationProperties.gameFolder.resolve(value.path.toPath()).takeIf { it.exists() }?.also {
                        val element = it to Files.createTempFile("MFR", "update")
                        element.first.moveTo(element.second, true)
                        backup.add(element)
                    }
                }
                taskFactory.fileDownloadTask().execute(files, progressBar)
            } catch (e: Exception) {
                log.error("Error while game update", e)
                backup.forEach { it.second.moveTo(it.first, true) }
            } finally {
                backup.forEach { it.second.deleteIfExists() }
            }
        }
        applicationStatus.gameVersion.update()
        progressBar.updateProgress(0)
        progressBar.updateDescription("Проверка состояния")
        propertiesService.updateValue(Properties.Key.LAST_UPDATE_DATE, startTime)
        taskFactory.fillSchemaTask().execute(progressBar)
        taskFactory.applyOptionsTask().execute(progressBar = progressBar)
        progressBar.hide()
    }

    companion object {
        private val log by Logger()
    }
}