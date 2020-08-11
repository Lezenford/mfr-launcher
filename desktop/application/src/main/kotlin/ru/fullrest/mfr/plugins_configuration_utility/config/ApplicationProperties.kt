package ru.fullrest.mfr.plugins_configuration_utility.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import ru.fullrest.mfr.plugins_configuration_utility.exception.ApplicationStartException
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

@Configuration
class ApplicationProperties(

    @Value("\${application.forum_link}")
    val forumLink: String,

    @Value("\${application.discord_link}")
    val discordLink: String,

    @Value("\${application.game.main.data_files}")
    val dataFiles: String,

    @Value("\${application.game.main.morrowind}")
    val morrowind: String,

    @Value("\${application.game.openmw.application}")
    val openMw: String,

    @Value("\${application.game.main.launcher}")
    val launcher: String,

    @Value("\${application.game.openmw.launcher}")
    val openMwLauncher: String,

    @Value("\${application.game.classic.mcp}")
    val mcp: String,

    @Value("\${application.game.classic.mge.application}")
    val mge: String,

    @Value("\${application.game.main.readme}")
    val readme: String,

    @Value("\${application.game.main.optional}")
    val optional: String,

    @Value("\${application.game.main.version_file}")
    val versionFileName: String,

    @Value("\${application.game.main.schema_file}")
    val schemaFileName: String,

    @Value("\${application.version}")
    val applicationVersion: String,

    @Value("\${application.platform}")
    val platform: String,

    @Value("\${application.game.main.update_link}")
    val updateLink: String,

    @Value("\${application.game.classic.mge.folder}")
    val mgeFolder: String,

    @Value("\${application.game.classic.mge.config.backup}")
    val mgeBackupCurrentConfig: String,

    @Value("\${application.game.classic.mge.config.current}")
    val mgeCurrentConfig: String,

    @Value("\${application.game.classic.mge.config.template.high_quality}")
    val highPerformanceMgeConfig: String,

    @Value("\${application.game.classic.mge.config.template.middle_quality}")
    val middlePerformanceMgeConfig: String,

    @Value("\${application.game.classic.mge.config.template.low_quality}")
    val lowPerformanceMgeConfig: String,

    @Value("\${application.game.classic.mge.config.template.basic_quality}")
    val necroPerformanceMgeConfig: String,

    @Value("\${application.game.openmw.config.folder}")
    val currentOpenMwConfigFolder: String,

    @Value("\${application.game.openmw.config.backup}")
    val currentBackupOpenMwConfigFolder: String,

    @Value("\${application.game.openmw.config.template.high_quality}")
    val highPerformanceOpenMwConfigFolder: String,

    @Value("\${application.game.openmw.config.template.middle_quality}")
    val middlePerformanceOpenMwConfigFolder: String,

    @Value("\${application.game.openmw.config.template.low_quality}")
    val lowPerformanceMgeOpenMwConfigFolder: String,

    @Value("\${application.game.openmw.config.template.basic_quality}")
    val necroPerformanceOpenMwConfigFolder: String,

    @Value("\${application.game.openmw.config.change_value}")
    val openMwPathChangeValue: String
) : Loggable {
    val openMwConfigFiles: List<String> = listOf("input_v3.xml", "openmw.cfg", "settings.cfg", "launcher.cfg")

    val gamePath: String = File("game").absoluteFile.toString()

    val beta = true

    lateinit var gameVersion: String

    fun initVersion() {
        File(gamePath + File.separator + versionFileName).also { versionFile ->
            if (versionFile.exists() && versionFile.isFile) {
                BufferedReader(FileReader(versionFile)).use { gameVersion = it.readLine() }
            } else {
                throw ApplicationStartException("Version file doesn't exist or isn't a file: ${versionFile.absolutePath}")
            }
        }
    }
}