package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.protocol.file.SCHEMA_FILE_NAME
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.config.properties.GameProperties
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.model.SectionService
import com.lezenford.mfr.launcher.service.provider.KtorProvider
import com.lezenford.mfr.schema.v1.File
import com.lezenford.mfr.schema.v1.Schema
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class GameUpdateTask(
    private val applicationProperties: ApplicationProperties,
    private val sectionService: SectionService,
    private val factory: TaskFactory,
    private val ktorProvider: KtorProvider,
) : Task<Unit, Unit>() {

    override suspend fun action(params: Unit) {
        updateDescription("Подготовка к обновлению")

        val version = ktorProvider.requireActiveGameVersion(installedLine())
        val versionDetails = ktorProvider.findGameVersionSchema(version)
        val schema = ktorProvider.findGameSchema(versionDetails.host, versionDetails.schema, versionDetails.compressedSchema)
        val filesPlan = ktorProvider.findGameFilesPlan(versionDetails.host, versionDetails.files, versionDetails.compressedFiles)
            .filesList.associateBy { it.path }

        val (filesForDownload, filesForRemove) = findContent(schema)

        filesForDownload.filter { it.mainPath !in filesPlan }.takeIf { it.isNotEmpty() }?.also {
            log.error("Some files don't have link for download. $it")
            throw IllegalArgumentException("Inconsistent files")
        }

        if (filesForDownload.isNotEmpty()) {
            // Ошибка загрузки должна остановить обновление до записи новой схемы: иначе
            // установка считается обновлённой, а часть файлов в ней не докачана.
            joinSubtask(
                factory.downloadFileTask(),
                DownloadFileTask.Properties(
                    host = versionDetails.host,
                    files = filesForDownload.map { file ->
                        DownloadFileTask.Properties.File(
                            mainPath = applicationProperties.gameFolder.resolve(file.mainPath),
                            optionalPath = file.takeIf { it.hasOptionalPath() }
                                ?.let { applicationProperties.gameFolder.resolve(it.optionalPath) },
                            storage = filesPlan.getValue(file.mainPath).storage,
                            sha256 = file.sha256.toByteArray(),
                            compressedStorage = filesPlan.getValue(file.mainPath).compressedStorageOrNull
                        )
                    },
                    applyOptionalPath = false
                )
            )
        }
        filesForRemove.forEach {
            applicationProperties.gameFolder.resolve(it.mainPath).deleteIfExists()
        }
        applicationProperties.gameFolder.resolve(SCHEMA_FILE_NAME).writeBytes(schema.toByteArray())
        State.schema.emit(schema)
        State.gameVersion.emit(schema.version)

        updateProgress(0)
        updateDescription("Проверка состояния")

        joinSubtask(factory.fillSchemaTask())

        val filesForApply = sectionService.findAllWithDetails().filter { it.downloaded }.map { section ->
            null to section.options.first { it.applied }
        }
        joinSubtask(factory.applyOptionsTask(), filesForApply)
    }

    private suspend fun findContent(schema: Schema): Pair<Collection<File>, Collection<File>> {
        val currentSchema = Schema.parseFrom(applicationProperties.gameFolder.resolve(SCHEMA_FILE_NAME).readBytes())
        val downloadedSections =
            sectionService.findAllWithDetails().filter { it.downloaded }.associateBy({ it.name }, { it.options.map { it.name } })

        val currentSchemaFiles = (currentSchema.partitionsList.flatMap { it.filesList } + currentSchema.optionsList.flatMap { option ->
            option.contentsList.filter { option.name in downloadedSections || it.partition.required }.flatMap { it.partition.filesList }
        }).associateBy { it.mainPath }

        val newSchemaFiles = (schema.partitionsList.flatMap { it.filesList } + schema.optionsList.flatMap { option ->
            option.contentsList.filter { option.name in downloadedSections || it.partition.required }.flatMap { it.partition.filesList }
        }).associateBy { it.mainPath }

        val filesForRemove = currentSchemaFiles.filter { it.key !in newSchemaFiles }.values

        // Дофильтруем файлы, которые не менялись с прошлого состояния
        val filesForDownload = newSchemaFiles.filter { currentSchemaFiles[it.key]?.sha256 != it.value.sha256 }.values

        return filesForDownload to filesForRemove
    }

    companion object {
        private val log by Logger()
    }
}