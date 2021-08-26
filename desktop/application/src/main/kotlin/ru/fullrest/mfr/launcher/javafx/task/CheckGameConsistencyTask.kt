package ru.fullrest.mfr.launcher.javafx.task

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.api.ContentType
import ru.fullrest.mfr.common.api.rest.Content
import ru.fullrest.mfr.common.extensions.md5
import ru.fullrest.mfr.common.extensions.toPath
import ru.fullrest.mfr.javafx.component.ProgressBar
import ru.fullrest.mfr.launcher.component.ApplicationStatus
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.javafx.TaskFactory
import ru.fullrest.mfr.launcher.model.entity.Properties
import ru.fullrest.mfr.launcher.service.ExtraService
import ru.fullrest.mfr.launcher.service.PropertiesService
import ru.fullrest.mfr.launcher.service.RestTemplateService
import ru.fullrest.mfr.launcher.service.SectionService
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class CheckGameConsistencyTask(
    private val applicationProperties: ApplicationProperties,
    private val applicationStatus: ApplicationStatus,
    private val restTemplateService: RestTemplateService,
    private val sectionService: SectionService,
    private val extraService: ExtraService,
    private val propertiesService: PropertiesService,
    private val taskFactory: TaskFactory,
    private val objectMapper: ObjectMapper
) {

    suspend fun execute(progressBar: ProgressBar) {
        progressBar.updateProgress(0)
        progressBar.updateDescription("Проверка целостности игры")
        val startDate = objectMapper.writeValueAsString(LocalDateTime.now(ZoneOffset.UTC))
        restTemplateService.content().also { content ->
            content.categories.flatMap { it.items }.flatMap { it.files }.filter { it.active.not() }.forEach {
                applicationProperties.gameFolder.resolve(it.path).deleteIfExists()
            }
            val downloadedSections = sectionService.findAll().filter { it.downloaded }
            val downloadedExtras = extraService.findAll().filter { it.downloaded }

            val sectionsForCheck: List<Content.Category.Item.File> =
                content.categories.find { it.type == ContentType.OPTIONAL }?.items?.filter { item ->
                    downloadedSections.any { it.name == item.name }
                }?.flatMap { it.files } ?: emptyList()

            val extrasForCheck: List<Content.Category.Item.File> =
                content.categories.find { it.type == ContentType.EXTRA }?.items?.filter { item ->
                    downloadedExtras.any { it.name == item.name }
                }?.flatMap { it.files } ?: emptyList()

            val filesForCheck = content.categories.first { it.type == ContentType.MAIN }.items.flatMap { it.files } +
                    sectionsForCheck + extrasForCheck

            val totalCount = filesForCheck.size.toLong()
            var currentCount = 0L

            val filesForDownload = filesForCheck.filterNot {
                progressBar.updateProgress(++currentCount, totalCount)
                val file = applicationProperties.gameFolder.resolve(it.path.toPath())
                file.exists() && file.md5().contentEquals(it.md5)
            }.associateBy { it.id }.toMutableMap()

            if (filesForDownload.isNotEmpty()) {
                progressBar.updateProgress(0)
                progressBar.updateDescription("Подготовка к скачиванию")
                taskFactory.fileDownloadTask().execute(filesForDownload, progressBar)
            }
            propertiesService.updateValue(Properties.Key.LAST_UPDATE_DATE, startDate)

            applicationStatus.gameInstalled.value = true

            progressBar.updateProgress(100)
            progressBar.updateDescription("Проверка состояния")
            taskFactory.fillSchemaTask().execute(progressBar)

            progressBar.hide()
        }
    }
}