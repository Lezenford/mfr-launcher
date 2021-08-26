package ru.fullrest.mfr.updater

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import ru.fullrest.mfr.common.extensions.toPath
import java.io.File
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolute
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.moveTo

class PluginConfigurationUtilityUpdater

const val fileNameProperty = "file_name"
const val launcherFileName = "M[FR] Launcher.exe"

fun main(args: Array<String>) {
    runBlocking {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            LogManager.getLogger(PluginConfigurationUtilityUpdater::class.java).error(e)
        }

        val fileName = args.find { it.startsWith(fileNameProperty) }?.split("=")?.last()
            ?: throw IllegalArgumentException("Argument is empty")
        val downloadedLauncher = fileName.toPath().takeIf { it.exists() }
            ?: throw IllegalArgumentException("Downloaded file doesn't exist")

        delay(5000)
        downloadedLauncher.moveTo(launcherFileName.toPath().absolute(), StandardCopyOption.REPLACE_EXISTING)

        downloadedLauncher.deleteIfExists()

        ProcessBuilder("\"$launcherFileName\"").also {
            it.directory(File("").absoluteFile)
        }.start()
        delay(3000)
    }
}