package ru.fullrest.mfr.launcher.javafx.task

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.extensions.toPath
import ru.fullrest.mfr.javafx.component.ProgressBar
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.exception.NotEnoughSpaceException
import ru.fullrest.mfr.launcher.javafx.TaskFactory
import ru.fullrest.mfr.launcher.model.entity.Properties
import ru.fullrest.mfr.launcher.service.PropertiesService
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.io.path.deleteIfExists

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class GameUpdateTask(
    private val applicationProperties: ApplicationProperties,
    private val taskFactory: TaskFactory,
    private val propertiesService: PropertiesService,
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
            taskFactory.fileDownloadTask().execute(files, progressBar)
        }
        progressBar.updateProgress(0)
        progressBar.updateDescription("Проверка состояния")
        propertiesService.updateValue(Properties.Key.LAST_UPDATE_DATE, startTime)
        taskFactory.fillSchemaTask().execute(progressBar)
        progressBar.hide()
    }
}