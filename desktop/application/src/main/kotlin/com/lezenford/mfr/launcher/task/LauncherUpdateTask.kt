package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.extensions.md5
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.exception.ApplicationException
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.provider.RestProvider
import kotlinx.coroutines.flow.firstOrNull
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class LauncherUpdateTask(
    private val restProvider: RestProvider,
    private val factory: TaskFactory,
    private val applicationProperties: ApplicationProperties
) : Task<Unit, Unit>() {

    override suspend fun action(params: Unit) {
        updateDescription("Подготовка к скачиванию")

        val client = restProvider.clientVersions().firstOrNull { it.system == applicationProperties.platform }
            ?: throw ApplicationException("Platform ${applicationProperties.platform} not found on server")

        val tempFile = joinSubtask(factory.downloadLauncherFileTask(), client)

        updateDescription("Подготовка к установке")
        updateProgress(100)

        if (tempFile.md5().contentEquals(client.md5)) {
            ProcessBuilder(
                "./jdk/bin/java.exe",
                "-jar",
                "\"$UPDATE_UTILITY\"",
                "\"file_name=${tempFile.absolutePathString()}\""
            ).apply {
                directory(applicationProperties.gameFolder.parent.toFile())
            }.start()
            exitProcess(0)
        }
    }

    companion object {
        private const val UPDATE_UTILITY = "launcher_update.jar"
    }
}