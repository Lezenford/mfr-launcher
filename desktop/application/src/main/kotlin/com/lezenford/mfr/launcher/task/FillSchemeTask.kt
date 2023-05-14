package com.lezenford.mfr.launcher.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.md5
import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.common.protocol.file.SCHEMA_FILE_NAME
import com.lezenford.mfr.common.protocol.file.Schema
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.model.entity.*
import com.lezenford.mfr.launcher.service.model.ExtraService
import com.lezenford.mfr.launcher.service.model.PropertiesService
import com.lezenford.mfr.launcher.service.model.SectionService
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import kotlin.io.path.exists
import kotlin.io.path.readBytes

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class FillSchemeTask(
    private val properties: ApplicationProperties,
    private val sectionService: SectionService,
    private val extraService: ExtraService,
    private val propertiesService: PropertiesService,
    private val objectMapper: ObjectMapper,
) : Task<Unit, Unit>() {
    override suspend fun action(params: Unit) {
        updateDescription("Обновление настраиваемых компонентов")
        val schemaFile = properties.gameFolder.resolve(SCHEMA_FILE_NAME.toPath()).also {
            if (it.exists().not()) {
                throw IllegalArgumentException("Schema file not found")
            }
        }
        val savedSchemaMd5 = propertiesService.findByKey(Properties.Key.SCHEMA)?.value
        val currentSchemaMd5 = schemaFile.md5().contentToString()
        if (currentSchemaMd5 != savedSchemaMd5) {
            log.info("The saved schema does not match the current one")
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
                                    files.addAll(
                                        option.items.map { item ->
                                            OptionFile(
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

                if (notAppliedSections.isNotEmpty()) {
                    updateDescription("Поиск активной конфигурации")
                    val totalCount = notAppliedSections.flatMap { it.options }.sumOf { it.files.size }.toLong()
                    var currentCount = 0L

                    notAppliedSections.forEach { section ->
                        section.options.find { option ->
                            val startCount = currentCount
                            option.files.all {
                                updateProgress(++currentCount, totalCount)
                                properties.gameFolder.resolve(it.gamePath.toPath())
                                    .takeIf { file -> file.exists() }?.md5()
                                    ?.contentEquals(it.md5) ?: false
                            }.also {
                                currentCount = option.files.size + startCount
                                updateProgress(currentCount, totalCount)
                            }
                        }?.applied = true
                    }
                }

                updateProgress(100)
                updateDescription("Сохранение настроек")

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