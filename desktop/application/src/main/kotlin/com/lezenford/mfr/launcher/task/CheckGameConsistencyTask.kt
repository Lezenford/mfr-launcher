package com.lezenford.mfr.launcher.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.lezenford.mfr.common.extensions.md5
import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.common.protocol.enums.ContentType
import com.lezenford.mfr.common.protocol.http.dto.Content
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.javafx.controller.QuestionController
import com.lezenford.mfr.launcher.model.entity.Properties
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.model.ExtraService
import com.lezenford.mfr.launcher.service.model.PropertiesService
import com.lezenford.mfr.launcher.service.model.SectionService
import com.lezenford.mfr.launcher.service.provider.RestProvider
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class CheckGameConsistencyTask(
    private val properties: ApplicationProperties,
    private val restProvider: RestProvider,
    private val sectionService: SectionService,
    private val extraService: ExtraService,
    private val propertiesService: PropertiesService,
    private val taskFactory: TaskFactory,
    private val objectMapper: ObjectMapper,
    private val questionControllerFactory: ObjectFactory<QuestionController>
) : Task<Unit, Unit>() {

    override suspend fun action(params: Unit) {
        updateDescription("Проверка целостности игры")
        val startDate = objectMapper.writeValueAsString(LocalDateTime.now(ZoneOffset.UTC))
        val content: Content = restProvider.findBuild(State.currentGameBuild.value)
        content.categories.flatMap { it.items }.flatMap { it.files }.filter { it.active.not() }.forEach {
            properties.gameFolder.resolve(it.path).deleteIfExists()
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
            updateProgress(++currentCount, totalCount)
            val file = properties.gameFolder.resolve(it.path.toPath())
            file.exists() && file.md5().contentEquals(it.md5)
        }.toMutableList()

        val settingsFiles = filesForDownload.filter { file -> SETTINGS_FILE.any { file.path.contains(it) } }
        if (settingsFiles.isNotEmpty()) {
            val response = questionControllerFactory.`object`
                .show(
                    description = """Некоторые файлы могут содержать настройки игры. 
                        |Их восстановление приведет к восстановлению настроек по умолчанию. 
                        |Хотите сбросить настройки?""".trimMargin()
                )
            if (response.not()) {
                filesForDownload.removeAll(settingsFiles)
            }
        }

        if (filesForDownload.isNotEmpty()) {
            filesForDownload.forEach {
                properties.gameFolder.resolve(it.path.toPath()).deleteIfExists()
            }

            joinSubtask(taskFactory.downloadGameFileTask(), filesForDownload.filter { it.active })
        }
        propertiesService.updateValue(Properties.Key.LAST_UPDATE_DATE, startDate)

        State.gameInstalled.emit(true)

        updateProgress(100)
        updateDescription("Проверка состояния")

        joinSubtask(taskFactory.fillSchemaTask())

        val filesForApply = sectionService.findAllWithDetails().filter { it.downloaded }.map { section ->
            null to section.options.first { it.applied }
        }
        joinSubtask(taskFactory.applyOptionsTask(), filesForApply)
    }

    companion object {
        private val SETTINGS_FILE =
            listOf("Morrowind.exe", "Morrowind.ini", "mge3\\MGE.ini", "Data Files\\MWSE\\config", "mcpatch\\installed")
    }
}