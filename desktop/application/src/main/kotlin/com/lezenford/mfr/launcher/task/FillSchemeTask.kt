package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.sha256
import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.extension.sha256
import com.lezenford.mfr.launcher.extension.toOldSchema
import com.lezenford.mfr.launcher.model.entity.Option
import com.lezenford.mfr.launcher.model.entity.OptionFile
import com.lezenford.mfr.launcher.model.entity.Properties
import com.lezenford.mfr.launcher.model.entity.Section
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.model.PropertiesService
import com.lezenford.mfr.launcher.service.model.SectionService
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import kotlin.io.path.exists

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class FillSchemeTask(
    private val properties: ApplicationProperties,
    private val sectionService: SectionService,
    private val propertiesService: PropertiesService,
    private val taskFactory: TaskFactory,
) : Task<Unit, Unit>() {
    override suspend fun action(params: Unit) {
        updateDescription("Обновление настраиваемых компонентов")
        val schema = State.schema.value ?: throw IllegalArgumentException("Schema file not found")
        val savedSchemaSha256 = propertiesService.findByKey(Properties.Key.SCHEMA)?.value
        val currentSchemaSha256 = schema.sha256().contentToString()
        if (currentSchemaSha256 != savedSchemaSha256) {
            log.info("The saved schema does not match the current one")
            val groups = sectionService.findAllWithDetails().toMutableList()

            val oldSchema = schema.toOldSchema()

            val sections = oldSchema.packages.map { `package` ->
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
                                            sha256 = item.md5,
                                            option = this
                                        )
                                    }
                                )
                            }
                        }
                    )
                }
            }

            val notAppliedSections = sections.filter { section -> section.options.none { it.applied } }

            val optionsForApply = mutableListOf<Option>()

            if (notAppliedSections.isNotEmpty()) {
                updateDescription("Поиск активной конфигурации")
                val totalCount = notAppliedSections.flatMap { it.options }.sumOf { it.files.size }.toLong()
                var currentCount = 0L

                notAppliedSections.forEach { section ->
                    val activeOptions = section.options.find { option ->
                        val startCount = currentCount
                        option.files.all {
                            updateProgress(++currentCount, totalCount)
                            properties.gameFolder.resolve(it.gamePath.toPath())
                                .takeIf { file -> file.exists() }?.sha256()
                                ?.contentEquals(it.sha256) ?: false
                        }.also {
                            currentCount = option.files.size + startCount
                            updateProgress(currentCount, totalCount)
                        }

                    }
                    val defaultOptionName = schema.optionsList.firstOrNull { it.name == section.name }
                        ?.contentsList?.firstOrNull { it.partition.required }?.name
                    val defaultOption = section.options.find { it.name == defaultOptionName }
                    activeOptions ?: defaultOption?.let { optionsForApply.add(it) }
                    (activeOptions ?: defaultOption)?.applied = true
                }
            }

            updateProgress(100)
            updateDescription("Сохранение настроек")

            sectionService.removeAll()
            sectionService.saveAll(sections)

            if (optionsForApply.isNotEmpty()){
                updateDescription("Применение опций по умолчанию")
                joinSubtask(taskFactory.applyOptionsTask(), optionsForApply.map { null to it })
                updateProgress(100)
            }

            propertiesService.updateValue(Properties.Key.SCHEMA, currentSchemaSha256)
        }
    }

    companion object {
        private val log by Logger()
    }
}