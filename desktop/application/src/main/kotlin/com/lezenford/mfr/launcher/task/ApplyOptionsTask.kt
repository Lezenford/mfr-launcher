package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.model.entity.Option
import com.lezenford.mfr.launcher.model.entity.OptionFile
import com.lezenford.mfr.launcher.service.model.OptionService
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class ApplyOptionsTask(
    private val properties: ApplicationProperties,
    private val optionService: OptionService,
) : Task<List<Pair<Option?, Option>>, Unit>() {

    override suspend fun action(params: List<Pair<Option?, Option>>) {
        val errorOptions: MutableMap<Option, List<OptionFile>> = mutableMapOf()
        val totalCount = params.sumOf { it.second.files.size }.toLong()
        var currentCount = 0L

        params.forEach { (currentOption, option) ->
            updateDescription("Установка пакета ${option.section.name} - ${option.name}")
            option.files.filterNot { item ->
                properties.gameFolder.resolve(item.storagePath.toPath()).exists()
            }.takeIf { it.isNotEmpty() }?.also {
                errorOptions[option] = it //TODO нужно больше информации, почему ошибка
                currentCount += option.files.size
                updateProgress(currentCount, totalCount)
            } ?: kotlin.run {
                val backupFiles: MutableList<Pair<Path, Path>> = mutableListOf()
                try {
                    currentOption?.files?.forEach {
                        val gamePath = properties.gameFolder.resolve(it.gamePath)
                        val backupPath = Files.createTempFile("MFR", option.name)
                        if (gamePath.exists()) {
                            backupFiles.add(gamePath to backupPath)
                            Files.move(gamePath, backupPath, StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                    option.files.forEach {
                        val gamePath = properties.gameFolder.resolve(it.gamePath)
                        val storagePath = properties.gameFolder.resolve(it.storagePath)
                        Files.copy(storagePath, gamePath, StandardCopyOption.REPLACE_EXISTING)
                        updateProgress(++currentCount, totalCount)
                    }

                    currentOption?.also {
                        it.applied = false
                        optionService.save(it)
                    }
                    option.applied = true
                    optionService.save(option)
                } catch (e: Exception) {
//                    progressBar.disable()
//                    progressBar.updateDescription("Ошибка установки")
                    currentOption?.also {
                        option.files.forEach {
                            properties.gameFolder.resolve(it.gamePath).deleteIfExists()
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
//            progressBar.disable()
            errorOptions.map { (key, value) -> key to value.map { it.storagePath } }.toMap().also { map ->
                throw IllegalArgumentException("That options doesn't apply: ${map.keys.joinToString(", ") { "${it.section.name} (${it.name})" }}")
            }
        } else {
            updateProgress(100)
        }
    }

    companion object {
        private val log by Logger()
    }
}