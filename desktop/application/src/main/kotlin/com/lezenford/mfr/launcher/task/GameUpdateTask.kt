package com.lezenford.mfr.launcher.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.common.protocol.enums.ContentType
import com.lezenford.mfr.common.protocol.http.dto.Content
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.config.properties.GameProperties
import com.lezenford.mfr.launcher.exception.NotEnoughSpaceException
import com.lezenford.mfr.launcher.model.entity.Properties
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.model.ExtraService
import com.lezenford.mfr.launcher.service.model.PropertiesService
import com.lezenford.mfr.launcher.service.model.SectionService
import com.lezenford.mfr.launcher.service.provider.RestProvider
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
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
    private val gameProperties: GameProperties,
    private val sectionService: SectionService,
    private val extraService: ExtraService,
    private val factory: TaskFactory,
    private val propertiesService: PropertiesService,
    private val objectMapper: ObjectMapper,
    private val restProvider: RestProvider,
) : Task<Unit, Unit>() {

    override suspend fun action(params: Unit) {
        updateDescription("Подготовка к обновлению")
        val startTime = objectMapper.writeValueAsString(LocalDateTime.now(ZoneOffset.UTC))

        val files = findContent()

        if (files.isNotEmpty()) {
            val freeSpace = (applicationProperties.gameFolder.root.toFile().usableSpace / 1024.0).toLong()
            val totalSpace = (files.sumOf { it.size } * 1.1 / 1024.0).toLong()
            if (freeSpace < totalSpace) {
                throw NotEnoughSpaceException("Для успешной установки необходимо минимум $totalSpace КБ свободного места. Доступно всего $freeSpace КБ. Установка будет прервана")
            }
            val backup = mutableListOf<Pair<Path, Path>>()
            try {
                files.forEach { file ->
                    applicationProperties.gameFolder.resolve(file.path.toPath()).takeIf { it.exists() }?.also {
                        val element = it to Files.createTempFile("MFR", "update")
                        element.first.moveTo(element.second, true)
                        backup.add(element)
                    }
                }

                //TODO нет проверки на удаление файлов
                joinSubtask(factory.downloadGameFileTask(), files)

            } catch (e: Exception) {
                log.error("Error while game update", e)
                backup.forEach { it.second.moveTo(it.first, true) }
            } finally {
                backup.forEach { it.second.deleteIfExists() }
            }
        }
        State.gameVersion.emit(gameProperties.version)

        updateProgress(0)
        updateDescription("Проверка состояния")
        propertiesService.updateValue(Properties.Key.LAST_UPDATE_DATE, startTime)

        joinSubtask(factory.fillSchemaTask())

        val filesForApply = sectionService.findAllWithDetails().filter { it.downloaded }.map { section ->
            null to section.options.first { it.applied }
        }
        joinSubtask(factory.applyOptionsTask(), filesForApply)
    }

    private suspend fun findContent(): List<Content.Category.Item.File> {
        return propertiesService.findByKey(Properties.Key.LAST_UPDATE_DATE)?.value?.let {
            objectMapper.readValue<LocalDateTime>(it)
        }?.let { lastUpdateDate ->
            val content = restProvider.findBuild(State.currentGameBuild.value, lastUpdateDate)
            val savedSections = sectionService.findAllWithDetails()
            val savedExtras = extraService.findAll()

            val newMain =
                content.categories.find { it.type == ContentType.MAIN }?.items?.flatMap { it.files } ?: emptyList()

            val newSections = savedSections.filter { it.downloaded }.mapNotNull { section ->
                content.categories.find { it.type == ContentType.OPTIONAL }?.let { `package` ->
                    `package`.items.find { it.name == section.name }?.files
                }
            }.flatten()

            val newExtras = savedExtras.filter { it.downloaded }.mapNotNull { extra ->
                content.categories.find { it.type == ContentType.EXTRA }?.let { `package` ->
                    `package`.items.find { it.name == extra.name }?.files
                }
            }.flatten()

            newMain + newSections + newExtras
        } ?: emptyList()
    }

    companion object {
        private val log by Logger()
    }
}