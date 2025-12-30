package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.service.provider.KtorProvider
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.io.File
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

        val tempFile = File.createTempFile("mfr_", version)
        ktorProvider.downloadFile(versionSchema.host, versionSchema.files).toInputStream().copyTo(tempFile.outputStream())

        updateDescription("Подготовка к установке")
        updateProgress(100)

        ProcessBuilder(
            "./jdk/bin/java.exe",
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