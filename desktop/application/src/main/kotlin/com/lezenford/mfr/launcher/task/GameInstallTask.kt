package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.exception.ServerMaintenanceException
import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.protocol.file.SCHEMA_FILE_NAME
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.model.entity.Properties
import com.lezenford.mfr.launcher.model.repository.ExtraRepository
import com.lezenford.mfr.launcher.service.MgeService
import com.lezenford.mfr.launcher.service.OpenMwService
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.model.PropertiesService
import com.lezenford.mfr.launcher.service.model.SectionService
import com.lezenford.mfr.launcher.service.provider.KtorProvider
import com.lezenford.mfr.launcher.service.runner.RunnerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import kotlin.io.path.writeBytes

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class GameInstallTask(
    private val properties: ApplicationProperties,
    private val factory: TaskFactory,
    private val mgeService: MgeService,
    private val openMwService: OpenMwService,
    private val propertiesService: PropertiesService,
    private val sectionService: SectionService,
    private val ktorProvider: KtorProvider,
    private val runnerService: RunnerService,
    private val extraRepository: ExtraRepository
) : Task<Unit, Unit>() {

    override suspend fun action(params: Unit) {
        updateDescription("Подготовка к установке")

        if (propertiesService.findByKey(Properties.Key.GAME_INSTALLED) != null && propertiesService.findByKey(Properties.Key.OLD_RELEASE_REMOVED) == null) {
            extraRepository.deleteAll()
            properties.gameFolder.toFile().listFiles()?.filter { it.name !in setOf("Saves", "screenshots") }?.forEach {
                if (it.exists()) {
                    it.deleteRecursively()
                }
            }
            propertiesService.save(Properties(Properties.Key.OLD_RELEASE_REMOVED))
        }

        // Свежая установка идёт в первую линию перечня: сервер отдаёт линии по убыванию.
        val line = State.selectedBuild.value
            ?: ktorProvider.findGameChannels().firstOrNull()
            ?: throw ServerMaintenanceException("Нет доступных линий игры")
        val version = ktorProvider.requireActiveGameVersion(line)
        log.info("Found version $version in line $line")
        val versionDetails = ktorProvider.findGameVersionSchema(version)
        log.info("Version can by downloaded from ${versionDetails.host}")
        val schema = ktorProvider.findGameSchema(versionDetails.host, versionDetails.schema, versionDetails.compressedSchema)
        val filesPlan = ktorProvider.findGameFilesPlan(versionDetails.host, versionDetails.files, versionDetails.compressedFiles)

        properties.gameFolder.also { it.toFile().mkdirs() }.resolve(SCHEMA_FILE_NAME).writeBytes(schema.toByteArray())
        State.schema.emit(schema)
        State.selectedBuild.emit(line)
        log.info("Saved schema file")

        val mandatoryFiles = (schema.partitionsList.filter { it.required }.flatMap { it.filesList } + schema.optionsList.asSequence()
            .flatMap { it.contentsList }.map { it.partition }.filter { it.required }.flatMap { it.filesList })

        val downloadPlan = filesPlan.filesList.associateBy { it.path }
        mandatoryFiles.filter { it.mainPath !in downloadPlan }.takeIf { it.isNotEmpty() }?.also {
            log.error("Some files don't have link for download. $it")
            throw IllegalArgumentException("Inconsistent files")
        }

        joinSubtask(
            factory.downloadFileTask(), DownloadFileTask.Properties(
                host = versionDetails.host,
                files = mandatoryFiles.map { file ->
                    DownloadFileTask.Properties.File(
                        mainPath = properties.gameFolder.resolve(file.mainPath),
                        optionalPath = file.takeIf { it.hasOptionalPath() }?.let { properties.gameFolder.resolve(it.optionalPath) },
                        storage = downloadPlan.getValue(file.mainPath).storage,
                        sha256 = file.sha256.toByteArray(),
                        compressedStorage = downloadPlan.getValue(file.mainPath).compressedStorageOrNull
                    )
                },
                applyOptionalPath = true
            )
        )

        State.gameVersion.emit(schema.version)

        val optionFiles = withContext(Dispatchers.IO) {
            sectionService.findAllWithDetails().flatMap { it.options }.filter { it.applied }.flatMap { it.files }
                .map { it.gamePath }.toSet()
        }

        updateProgress(100)
        updateDescription("Анализ схемы")
        if (optionFiles.isNotEmpty()) {
            joinSubtask(factory.checkGameConsistencyTask())
        } else {
            joinSubtask(factory.fillSchemaTask())
        }

        propertiesService.updateValue(Properties.Key.NEW_RELEASE_INSTALLED)
        State.gameInstalled.emit(true)

        mgeService.applyConfig(MgeService.Configuration.MIDDLE, false)
        openMwService.prepareTemplates()
        openMwService.applyConfig(OpenMwService.Configuration.MIDDLE, false)

        runnerService.startMge().also {
            delay(4000)
        }.destroy()
    }

    companion object {
        private val log by Logger()
    }
}
