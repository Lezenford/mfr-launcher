package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.extensions.sha256
import com.lezenford.mfr.common.protocol.file.SCHEMA_FILE_NAME
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.javafx.controller.QuestionController
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.model.SectionService
import com.lezenford.mfr.launcher.service.provider.KtorProvider
import com.lezenford.mfr.schema.v1.File
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class CheckGameConsistencyTask(
    private val properties: ApplicationProperties,
    private val sectionService: SectionService,
    private val taskFactory: TaskFactory,
    private val questionControllerFactory: ObjectFactory<QuestionController>,
    private val ktorProvider: KtorProvider
) : Task<Unit, Unit>() {

    override suspend fun action(params: Unit) {
        updateDescription("Проверка целостности игры")

        val version = ktorProvider.requireActiveGameVersion(installedLine())
        val versionSchema = ktorProvider.findGameVersionSchema(version)
        val schema = ktorProvider.findGameSchema(versionSchema.host, versionSchema.schema, versionSchema.compressedSchema)
        val filesPlan = ktorProvider.findGameFilesPlan(versionSchema.host, versionSchema.files, versionSchema.compressedFiles)
            .filesList.associateBy { it.path }

        properties.gameFolder.resolve(SCHEMA_FILE_NAME).writeBytes(schema.toByteArray())

        val downloadedSections =
            sectionService.findAll().filter { it.downloaded }.associateBy({ it.name }, { it.options })
        val actualFiles = schema.partitionsList.flatMap { it.filesList } +
                schema.optionsList.flatMap { option ->
                    option.contentsList.filter { option.name in downloadedSections || it.partition.required }
                        .flatMap { it.partition.filesList }
                }

        updateDescription("Проверяем файлы")
        updateProgress(0)

        val incorrectFiles = LinkedBlockingQueue<File>()
        coroutineScope {
            val counter = AtomicInteger(0)
            val semaphore = Semaphore(10)
            actualFiles.forEach { file ->
                launch {
                    semaphore.withPermit {
                        val paths = properties.gameFolder.resolve(file.mainPath)
                        if (!paths.exists() || !paths.sha256().contentEquals(file.sha256.toByteArray())) {
                            incorrectFiles.add(file)
                        }
                        updateProgress(counter.incrementAndGet(), actualFiles.size)
                    }
                }
            }
        }
        updateProgress(100)

        if (incorrectFiles.isNotEmpty()) {
            val settingFiles = incorrectFiles.filter { file -> SETTINGS_FILE.any { file.mainPath.contains(it) } }
            if (settingFiles.isNotEmpty()) {
                val response = questionControllerFactory.`object`
                    .show(
                        description = """Некоторые файлы могут содержать настройки игры. 
                        |Их восстановление приведет к восстановлению настроек по умолчанию. 
                        |Хотите сбросить настройки?""".trimMargin()
                    )
                if (response.not()) {
                    incorrectFiles.removeAll(settingFiles)
                }
            }

            joinSubtask(
                taskFactory.downloadFileTask(), DownloadFileTask.Properties(
                    host = versionSchema.host,
                    files = incorrectFiles.map { file ->
                        DownloadFileTask.Properties.File(
                            mainPath = properties.gameFolder.resolve(file.mainPath),
                            optionalPath = file.takeIf { it.hasOptionalPath() }
                                ?.let { properties.gameFolder.resolve(it.optionalPath) },
                            storage = filesPlan.getValue(file.mainPath).storage,
                            sha256 = file.sha256.toByteArray(),
                            compressedStorage = filesPlan.getValue(file.mainPath).compressedStorageOrNull
                        )
                    },
                    applyOptionalPath = false
                )
            )

            State.gameInstalled.emit(true)
        }
        updateDescription("Проверка состояния")

        joinSubtask(taskFactory.fillSchemaTask())

        val filesForApply =
            sectionService.findAllWithDetails().filter { it.downloaded && it.options.any { it.applied } }
                .map { section ->
                    null to section.options.first { it.applied }
                }
        joinSubtask(taskFactory.applyOptionsTask(), filesForApply)
    }

    companion object {
        private val SETTINGS_FILE =
            setOf("Morrowind.exe", "Morrowind.ini", "mge3/MGE.ini", "Data Files/MWSE/config", "mcpatch/installed")
    }
}