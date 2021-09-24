package ru.fullrest.mfr.launcher.javafx.task

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.delay
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.api.ContentType
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.javafx.component.ProgressBar
import ru.fullrest.mfr.launcher.component.ApplicationStatus
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.config.properties.GameProperties
import ru.fullrest.mfr.launcher.exception.NotEnoughSpaceException
import ru.fullrest.mfr.launcher.javafx.TaskFactory
import ru.fullrest.mfr.launcher.javafx.controller.MgeController
import ru.fullrest.mfr.launcher.javafx.controller.OpenMwController
import ru.fullrest.mfr.launcher.model.entity.Properties
import ru.fullrest.mfr.launcher.service.PropertiesService
import ru.fullrest.mfr.launcher.service.RestTemplateService
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.io.path.absolutePathString

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class GameInstallTask(
    private val applicationProperties: ApplicationProperties,
    private val restTemplateService: RestTemplateService,
    private val gameProperties: GameProperties,
    private val taskFactory: TaskFactory,
    private val applicationStatus: ApplicationStatus,
    private val mgeController: MgeController,
    private val openMwController: OpenMwController,
    private val propertiesService: PropertiesService,
    private val objectMapper: ObjectMapper
) {

    suspend fun execute(progressBar: ProgressBar) {
        try {
            progressBar.updateProgress(0)
            progressBar.updateDescription("Подготовка к установке")

            val startDateTime = objectMapper.writeValueAsString(LocalDateTime.now(ZoneOffset.UTC))
            val content = restTemplateService.content()

            val freeSpace = (applicationProperties.gameFolder.root.toFile().usableSpace / 1024.0).toLong()
            val totalNeedSpace = (content.categories.first { it.type == ContentType.MAIN }.items.flatMap { it.files }
                .sumOf { it.size } * 1.1 / 1024.0).toLong()

            if (freeSpace < totalNeedSpace) {
                throw NotEnoughSpaceException("Для успешной установки необходимо минимум $totalNeedSpace КБ свободного места. Доступно всего $freeSpace КБ. Установка будет прервана")
            }

            taskFactory.fileDownloadTask().execute(
                progressBar = progressBar,
                requestFiles = content.categories.first { it.type == ContentType.MAIN }.items
                    .flatMap { it.files }
                    .filter { it.active }
                    .associateBy { it.id }
                    .toMutableMap()
            )

            progressBar.updateProgress(100)
            progressBar.updateDescription("Анализ схемы")
            taskFactory.fillSchemaTask().execute(progressBar)
            propertiesService.updateValue(Properties.Key.LAST_UPDATE_DATE, startDateTime)
            applicationStatus.gameInstalled.value = true
        } finally {
            progressBar.hide()
        }

        mgeController.useTemplate(gameProperties.classic.mge.templates.middle, false)
        openMwController.prepareTemplates()
        openMwController.useTemplate(gameProperties.openMw.templates.middle, false)

        ProcessBuilder("\"${gameProperties.classic.mge.application.absolutePathString()}\"").also {
            it.directory(applicationProperties.gameFolder.toFile())
        }.start().also {
            delay(4000)
        }.destroy()
    }

    companion object {
        private val log by Logger()
    }
}
