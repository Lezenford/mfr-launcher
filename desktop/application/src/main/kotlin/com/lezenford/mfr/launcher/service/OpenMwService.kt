package com.lezenford.mfr.launcher.service

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.md5
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.config.properties.GameProperties
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.*

@Service
class OpenMwService(
    private val gameProperties: GameProperties,
    private val applicationProperties: ApplicationProperties
) {

    suspend fun applyConfig(configuration: Configuration, backup: Boolean) {
        if (backup) {
            copyBackup()
        }
        //Применять только если у конфига есть все требуемые файлы
        configuration.toPath().takeIf { configFolder -> configFolder.configFiles().all { it.exists() } }
            //Удалить текущий конфиг
            ?.also { gameProperties.openMw.configFolder.configFiles().forEach { it.deleteIfExists() } }
            ?.configFiles()?.forEach {
                it.copyTo(gameProperties.openMw.configFolder.resolve(it.fileName), overwrite = true)
            } ?: log.error("Config $configuration doesn't exist")
    }

    suspend fun prepareTemplates() {
        val generator: (Path) -> Unit = { folder ->
            folder.resolve(templateConfigFileName).takeIf { it.exists() }?.also { template ->
                template.readLines().map {
                    it.replace(
                        gameProperties.openMw.configChangeValue,
                        applicationProperties.gameFolder.absolutePathString()
                    )
                }.also {
                    folder.resolve(configFileName).writeLines(it)
                }
            }
        }

        generator(gameProperties.openMw.templates.basic)
        generator(gameProperties.openMw.templates.low)
        generator(gameProperties.openMw.templates.middle)
        generator(gameProperties.openMw.templates.high)
    }

    suspend fun findActiveConfig(): Configuration? {
        return gameProperties.openMw.configFolder
            //Сравнивать только если существует текущий конфиг
            .takeIf { configFolder -> configFolder.configFiles().all { it.exists() } }
            ?.let { current ->
                listOf(
                    gameProperties.openMw.templates.high to Configuration.HIGH,
                    gameProperties.openMw.templates.middle to Configuration.MIDDLE,
                    gameProperties.openMw.templates.low to Configuration.LOW,
                    gameProperties.openMw.templates.basic to Configuration.BASIC
                ).find { current.equalsConfig(it.first) }?.second ?: Configuration.CUSTOM
            }
    }

    private fun copyBackup() {
        gameProperties.openMw.takeIf { openMw ->
            listOf(
                openMw.templates.high,
                openMw.templates.middle,
                openMw.templates.low,
                openMw.templates.basic
            ).none { it.equalsConfig(openMw.configFolder) }
        }?.also { openMw ->
            openMw.configFolder.configFiles()
                .takeIf { files -> files.all { it.exists() } }?.forEach {
                    it.copyTo(
                        openMw.configBackupFolder.resolve(it.fileName).apply { parent?.createDirectories() },
                        overwrite = true
                    )
                }
        }
    }

    private fun Path.configFiles() =
        configFiles.map {
            this.resolve(it).absolute()
        }

    private fun Path.equalsConfig(target: Path?): Boolean {
        return this.configFiles().let { source ->
            source.all { path ->
                path.takeIf { it.exists() }?.md5()
                    ?.contentEquals(target?.resolve(path.fileName)?.takeIf { it.exists() }?.md5()) ?: false
            }
        }
    }

    private fun Configuration.toPath(): Path = when (this) {
        Configuration.HIGH -> gameProperties.openMw.templates.high
        Configuration.MIDDLE -> gameProperties.openMw.templates.middle
        Configuration.LOW -> gameProperties.openMw.templates.low
        Configuration.BASIC -> gameProperties.openMw.templates.basic
        Configuration.CUSTOM -> gameProperties.openMw.configBackupFolder
    }

    enum class Configuration {
        HIGH, MIDDLE, LOW, BASIC, CUSTOM
    }

    companion object {
        private val log by Logger()
        private const val configFileName = "openmw.cfg"
        private val configFiles = listOf("input_v3.xml", "launcher.cfg", configFileName, "settings.cfg")
        private const val templateConfigFileName = "openmw_template.cfg"
    }
}