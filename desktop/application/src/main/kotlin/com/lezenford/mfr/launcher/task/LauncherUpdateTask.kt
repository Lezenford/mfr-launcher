package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.service.provider.KtorProvider
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import kotlin.system.exitProcess

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class LauncherUpdateTask(
    private val applicationProperties: ApplicationProperties,
    private val ktorProvider: KtorProvider
) : Task<Unit, Unit>() {

    override suspend fun action(params: Unit) {
        updateDescription("Подготовка к скачиванию")
        val version = ktorProvider.findActiveLauncherVersion()
        val versionSchema = ktorProvider.findLauncherVersionSchema(version)

        updateDescription("Идет скачивание")

        if (!File(versionSchema.jdk).exists()) {
            withContext(Dispatchers.IO) {
                val tempFile = Files.createTempFile("jdk_zip_", version).toFile()
                ktorProvider.downloadFile(versionSchema.host, versionSchema.jdkStorage).toInputStream()
                    .copyTo(tempFile.outputStream())
                val jdkDirectory = Files.createTempDirectory("jdk_").toFile()
                try {
                    ZipFile(tempFile).use { zip ->
                        zip.entries().asSequence().forEach { entry ->
                            val outputFile = File(jdkDirectory, entry.name)

                            if (entry.isDirectory) {
                                outputFile.mkdirs()
                            } else {
                                outputFile.parentFile?.mkdirs()
                                zip.getInputStream(entry).copyTo(outputFile.outputStream())
                            }
                        }
                    }
                    jdkDirectory.copyRecursively(File(versionSchema.jdk), true)
                } finally {
                    jdkDirectory.deleteRecursively()
                    tempFile.delete()
                }
            }
        }

        val tempFile = Files.createTempFile("mfr_", version).toFile()
        ktorProvider.downloadFile(versionSchema.host, versionSchema.launcherStorage).toInputStream()
            .copyTo(tempFile.outputStream())

        updateDescription("Подготовка к установке")
        updateProgress(100)

        ProcessBuilder(
            "./${versionSchema.jdk}/bin/java.exe",
            "-jar",
            "\"$UPDATE_UTILITY\"",
            "\"file_name=${tempFile.absolutePath}\""
        ).apply {
            directory(applicationProperties.gameFolder.parent.toFile())
        }.start()
        exitProcess(0)
    }

    companion object {
        private const val UPDATE_UTILITY = "launcher_update.jar"
    }
}