package ru.fullrest.mfr.launcher.javafx.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.api.json.SCHEMA_FILE_NAME
import ru.fullrest.mfr.common.api.json.Schema
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.md5
import ru.fullrest.mfr.common.extensions.toPath
import ru.fullrest.mfr.javafx.component.ProgressBar
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.model.entity.Extra
import ru.fullrest.mfr.launcher.model.entity.ExtraFile
import ru.fullrest.mfr.launcher.model.entity.Item
import ru.fullrest.mfr.launcher.model.entity.Option
import ru.fullrest.mfr.launcher.model.entity.Properties
import ru.fullrest.mfr.launcher.model.entity.Section
import ru.fullrest.mfr.launcher.service.ExtraService
import ru.fullrest.mfr.launcher.service.PropertiesService
import ru.fullrest.mfr.launcher.service.SectionService
import kotlin.io.path.exists
import kotlin.io.path.readBytes

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class FillSchemeTask(
    private val applicationProperties: ApplicationProperties,
    private val sectionService: SectionService,
    private val extraService: ExtraService,
    private val propertiesService: PropertiesService,
    private val objectMapper: ObjectMapper
) {
    suspend fun execute(progressBar: ProgressBar) {
        progressBar.updateProgress(0)
        progressBar.updateDescription("Обновление настраиваемых компонентов")
        val schemaFile = applicationProperties.gameFolder.resolve(SCHEMA_FILE_NAME.toPath()).also {
            if (it.exists().not()) {
                throw IllegalArgumentException("Schema file not found")
            }
        }
        val savedSchemaMd5 = propertiesService.findByKey(Properties.Key.SCHEMA)?.value
        val currentSchemaMd5 = schemaFile.md5().contentToString()
        if (currentSchemaMd5 != savedSchemaMd5) {
            val groups = sectionService.findAllWithDetails().toMutableList()
            val savedExtras = extraService.findAll().toMutableList()
            objectMapper.readValue<Schema>(schemaFile.readBytes()).also { schema ->
                val sections = schema.packages.map { `package` ->
                    val savedSection = groups.find { it.name == `package`.name }
                    Section(
                        name = `package`.name,
                        downloaded = savedSection?.downloaded ?: false
                    ).apply {
                        options.addAll(
                            `package`.options.map { option ->
                                val savedOption = savedSection?.options?.find { it.name == option.name }
                                Option(
                                    name = option.name,
                                    description = option.description,
                                    image = option.image,
                                    section = this,
                                    applied = savedOption?.applied ?: false
                                ).apply {
                                    items.addAll(
                                        option.items.map { item ->
                                            Item(
                                                storagePath = item.storagePath,
                                                gamePath = item.gamePath,
                                                md5 = item.md5,
                                                option = this
                                            )
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
                val extras = schema.extra.map { incomingExtra ->
                    val savedExtra = savedExtras.find { it.name == incomingExtra.name }
                    Extra(
                        name = incomingExtra.name,
                        downloaded = savedExtra?.downloaded ?: false
                    ).also { createdExtra ->
                        incomingExtra.items.map { item ->
                            ExtraFile(
                                path = item.path,
                                md5 = item.md5,
                                extra = createdExtra
                            )
                        }.also {
                            createdExtra.files.addAll(it)
                        }
                    }
                }

                val notAppliedSections = sections.filter { section -> section.options.none { it.applied } }
                val alreadyAppliedSections =
                    sections.filter { section -> section.options.any { it.applied && section.downloaded } }

                progressBar.updateDescription("Поиск активной конфигурации")
                val totalCount = notAppliedSections.flatMap { it.options }.sumOf { it.items.size }.toLong()
                var currentCount = 0L

                notAppliedSections.forEach { section ->
                    section.options.find { option ->
                        val startCount = currentCount
                        option.items.all {
                            progressBar.updateProgress(++currentCount, totalCount)
                            applicationProperties.gameFolder.resolve(it.gamePath.toPath())
                                .takeIf { file -> file.exists() }?.md5()
                                ?.contentEquals(it.md5) ?: false
                        }.also {
                            currentCount = option.items.size + startCount
                            progressBar.updateProgress(currentCount, totalCount)
                        }
                    }?.applied = true
                }

                progressBar.updateProgress(100)
                progressBar.updateDescription("Сохранение настроек")

                sectionService.removeAll()
                sectionService.saveAll(sections)
                extraService.removeAll()
                extraService.saveAll(extras)

                propertiesService.updateValue(Properties.Key.SCHEMA, currentSchemaMd5)
            }
        }
    }

    companion object {
        private val log by Logger()
    }
}