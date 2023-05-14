package com.lezenford.mfr.launcher.service

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.md5
import com.lezenford.mfr.launcher.config.properties.GameProperties
import com.lezenford.mfr.launcher.service.runner.RunnerService
import org.springframework.stereotype.Service
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

@Service
class MgeService(
    private val gameProperties: GameProperties,
    private val runnerService: RunnerService
) {
    suspend fun startMge(): Process {
        log.info("Try to open MGE configuration window")
        return runnerService.startMge()
    }

    suspend fun applyConfig(configuration: Configuration, backup: Boolean) {
        if (backup) {
            copyBackup()
        }
        configuration.toPath().takeIf { it.exists() }?.copyTo(gameProperties.classic.mge.config, overwrite = true)
            ?: log.error("Config $configuration doesn't exist")
        log.info("MGE configuration $configuration successfully applied")
    }

    suspend fun findActiveConfig(): Configuration? {
        val result = gameProperties.classic.mge.config.takeIf { it.exists() }?.md5()?.let { current ->
            listOf(
                gameProperties.classic.mge.templates.high.md5() to Configuration.HIGH,
                gameProperties.classic.mge.templates.middle.md5() to Configuration.MIDDLE,
                gameProperties.classic.mge.templates.low.md5() to Configuration.LOW,
                gameProperties.classic.mge.templates.basic.md5() to Configuration.BASIC
            ).find { current.contentEquals(it.first) }?.second ?: Configuration.CUSTOM
        }
        log.info("Find active MGE configuration: $result")
        return result
    }

    private fun copyBackup() {
        gameProperties.classic.mge.also {
            it.config.copyTo(it.configBackup.apply { parent?.createDirectories() }, overwrite = true)
        }
        log.info("MGE backup successfully finished")
    }

    private fun Configuration.toPath(): Path = when (this) {
        Configuration.HIGH -> gameProperties.classic.mge.templates.high
        Configuration.MIDDLE -> gameProperties.classic.mge.templates.middle
        Configuration.LOW -> gameProperties.classic.mge.templates.low
        Configuration.BASIC -> gameProperties.classic.mge.templates.basic
        Configuration.CUSTOM -> gameProperties.classic.mge.configBackup
    }

    enum class Configuration {
        HIGH, MIDDLE, LOW, BASIC, CUSTOM
    }

    companion object {
        private val log by Logger()
    }
}