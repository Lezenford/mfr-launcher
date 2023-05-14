package com.lezenford.mfr.launcher.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.lezenford.mfr.common.protocol.enums.ContentType
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.config.properties.GameProperties
import com.lezenford.mfr.launcher.exception.NotEnoughSpaceException
import com.lezenford.mfr.launcher.model.entity.Properties
import com.lezenford.mfr.launcher.service.MgeService
import com.lezenford.mfr.launcher.service.OpenMwService
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.model.PropertiesService
import com.lezenford.mfr.launcher.service.model.SectionService
import com.lezenford.mfr.launcher.service.provider.RestProvider
import com.lezenford.mfr.launcher.service.runner.RunnerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class GameInstallTask(
    private val properties: ApplicationProperties,
    private val restProvider: RestProvider,
    private val gameProperties: GameProperties,
    private val factory: TaskFactory,
    private val mgeService: MgeService,
    private val openMwService: OpenMwService,
    private val propertiesService: PropertiesService,
    private val sectionService: SectionService,
    private val objectMapper: ObjectMapper,
    private val runnerService: RunnerService
) : Task<Unit, Unit>() {

    override suspend fun action(params: Unit) {
        updateDescription("Подготовка к установке")

        val currentDateTime = LocalDateTime.now(ZoneOffset.UTC)
        val startDateTime = objectMapper.writeValueAsString(currentDateTime)

        val content = restProvider.findBuild(State.currentGameBuild.first { it > 0 })

        val freeSpace = (properties.gameFolder.root.toFile().usableSpace / 1024.0).toLong()
        val totalNeedSpace =
            (content.categories.first { it.type == ContentType.MAIN }.items.flatMap { it.files }
                .sumOf { it.size } * 1.1 / 1024.0).toLong()

        if (freeSpace < totalNeedSpace) {
            throw NotEnoughSpaceException("Для успешной установки необходимо минимум $totalNeedSpace КБ свободного места. Доступно всего $freeSpace КБ. Установка будет прервана")
        }

        val optionFiles = withContext(Dispatchers.IO) {
            sectionService.findAllWithDetails().flatMap { it.options }.filter { it.applied }.flatMap { it.files }
                .map { it.gamePath }.toSet()
        }

        val files = content.categories.first { it.type == ContentType.MAIN }.items
            .flatMap { it.files }
            .filter { it.active }
            .filterNot { optionFiles.contains(it.path) }

        joinSubtask(factory.downloadGameFileTask(), files)

        State.gameVersion.emit(gameProperties.version)

        updateProgress(100)
        updateDescription("Анализ схемы")
        if (optionFiles.isNotEmpty()) {
            joinSubtask(factory.checkGameConsistencyTask())
        } else {
            joinSubtask(factory.fillSchemaTask())
        }

        propertiesService.updateValue(Properties.Key.LAST_UPDATE_DATE, startDateTime)
        State.gameUpdateStatus.emit(State.gameUpdateStatus.value.copy(currentUpdateDate = currentDateTime))
        propertiesService.updateValue(Properties.Key.GAME_INSTALLED)
        State.gameInstalled.emit(true)

        mgeService.applyConfig(MgeService.Configuration.MIDDLE, false)
        openMwService.prepareTemplates()
        openMwService.applyConfig(OpenMwService.Configuration.MIDDLE, false)

        runnerService.startMge().also {
            delay(4000)
        }.destroy()
    }
}
