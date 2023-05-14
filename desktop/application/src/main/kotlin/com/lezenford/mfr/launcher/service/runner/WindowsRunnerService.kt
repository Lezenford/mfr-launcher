package com.lezenford.mfr.launcher.service.runner

import com.lezenford.mfr.launcher.Launcher
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.config.properties.GameProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import kotlin.io.path.absolutePathString

@Service
@ConditionalOnProperty(name = ["application.platform"], havingValue = "WINDOWS")
class WindowsRunnerService(
    override val applicationProperties: ApplicationProperties,
    override val application: Launcher,
    private val gameProperties: GameProperties
) : RunnerService() {
    override fun startClassicGame() {
        Runtime.getRuntime().exec(prepareCommand(gameProperties.classic.application.absolutePathString()))
    }

    override fun startClassicLauncher() {
        Runtime.getRuntime().exec(prepareCommand(gameProperties.classic.launcher.absolutePathString()))
    }

    override fun startMge(): Process {
        return ProcessBuilder("\"${gameProperties.classic.mge.application.absolutePathString()}\"").also {
            it.directory(applicationProperties.gameFolder.toFile())
        }.start()
    }

    override fun startMcp() {
        Runtime.getRuntime().exec(prepareCommand(gameProperties.classic.mcp.absolutePathString(), true))
    }

    override fun startOpenMwGame() {
        Runtime.getRuntime().exec(prepareCommand(gameProperties.openMw.application.absolutePathString()))
    }

    override fun startOpenMwLauncher() {
        Runtime.getRuntime().exec(prepareCommand(gameProperties.openMw.launcher.absolutePathString()))
    }

    private fun prepareCommand(command: String, elevate: Boolean = false): String {
        return "powershell.exe -Command \"Start-Process '${command.normalize()}' -WorkingDirectory '${
            applicationProperties.gameFolder.absolutePathString().normalize()
        }' ${if (elevate) "-Verb runAs" else ""}\""
    }

    private fun String.normalize(): String = this.replace("]", "`]").replace("[", "`[")
}