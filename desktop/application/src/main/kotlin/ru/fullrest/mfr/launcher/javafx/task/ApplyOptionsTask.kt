package ru.fullrest.mfr.launcher.javafx.task

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.toPath
import ru.fullrest.mfr.javafx.component.ProgressBar
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.model.entity.Item
import ru.fullrest.mfr.launcher.model.entity.Option
import ru.fullrest.mfr.launcher.service.OptionService
import ru.fullrest.mfr.launcher.service.SectionService
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class ApplyOptionsTask(
    private val applicationProperties: ApplicationProperties,
    private val optionService: OptionService,
    private val sectionService: SectionService
) {

    suspend fun execute(progressBar: ProgressBar) =
        execute(
            progressBar = progressBar,
            options = sectionService.findAllWithDetails().filter { it.downloaded }.map { section ->
                null to section.options.first { it.applied }
            })

    suspend fun execute(progressBar: ProgressBar, options: List<Pair<Option?, Option>>) {
        val errorOptions: MutableMap<Option, List<Item>> = mutableMapOf()
        val totalCount = options.sumOf { it.second.items.size }.toLong()
        var currentCount = 0L
        progressBar.updateProgress(0)

        options.forEach { (currentOption, option) ->
            progressBar.updateDescription("Установка пакета ${option.section.name} - ${option.name}")
            option.items.filterNot { item ->
                applicationProperties.gameFolder.resolve(item.storagePath.toPath()).exists()
            }.takeIf { it.isNotEmpty() }?.also {
                errorOptions[option] = it
                currentCount += option.items.size
                progressBar.updateProgress(currentCount, totalCount)
            } ?: kotlin.run {
                val backupFiles: MutableList<Pair<Path, Path>> = mutableListOf()
                try {
                    currentOption?.items?.forEach {
                        val gamePath = applicationProperties.gameFolder.resolve(it.gamePath)
                        val backupPath = Files.createTempFile("MFR", option.name)
                        if (gamePath.exists()) {
                            backupFiles.add(gamePath to backupPath)
                            Files.move(gamePath, backupPath, StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                    option.items.forEach {
                        val gamePath = applicationProperties.gameFolder.resolve(it.gamePath)
                        val storagePath = applicationProperties.gameFolder.resolve(it.storagePath)
                        Files.copy(storagePath, gamePath, StandardCopyOption.REPLACE_EXISTING)
                        progressBar.updateProgress(++currentCount, totalCount)
                    }

                    currentOption?.also {
                        it.applied = false
                        optionService.save(it)
                    }
                    option.applied = true
                    optionService.save(option)
                } catch (e: Exception) {
                    progressBar.disable()
                    progressBar.updateDescription("Ошибка установки")
                    currentOption?.also {
                        option.items.forEach {
                            applicationProperties.gameFolder.resolve(it.gamePath).deleteIfExists()
                        }
                    }
                    backupFiles.forEach { (gamePath, backupPath) ->
                        Files.move(backupPath, gamePath, StandardCopyOption.REPLACE_EXISTING)
                    }
                    throw IllegalArgumentException(
                        "Error while apply option ${option.section.name} - ${option.name}", e
                    )
                } finally {
                    backupFiles.forEach { it.second.deleteIfExists() }
                }
            }
        }
        if (errorOptions.isNotEmpty()) {
            progressBar.disable()
            errorOptions.map { (key, value) -> key to value.map { it.storagePath } }.toMap().also { map ->
                throw IllegalArgumentException("That options doesn't apply: ${map.keys.joinToString(", ") { "${it.section.name} (${it.name})" }}")
            }
        } else {
            progressBar.updateProgress(100)
        }
    }

    companion object {
        private val log by Logger()
    }
}