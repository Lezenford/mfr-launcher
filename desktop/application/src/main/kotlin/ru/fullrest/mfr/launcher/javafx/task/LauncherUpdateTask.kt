package ru.fullrest.mfr.launcher.javafx.task

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.api.rest.Content
import ru.fullrest.mfr.common.extensions.md5
import ru.fullrest.mfr.javafx.component.ProgressBar
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.javafx.TaskFactory
import ru.fullrest.mfr.launcher.service.RestTemplateService
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.system.exitProcess

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class LauncherUpdateTask(
    private val restTemplateService: RestTemplateService,
    private val taskFactory: TaskFactory,
    private val applicationProperties: ApplicationProperties
) {

    suspend fun execute(progressBar: ProgressBar) {
        progressBar.updateProgress(0)
        progressBar.updateDescription("Подготовка к установке")

        val client = restTemplateService.client()
        val tempFile = Files.createTempFile("MFR", "Launcher")
        taskFactory.fileDownloadTask().execute(
            requestFiles = mutableMapOf(
                0 to Content.Category.Item.File(
                    id = 0,
                    path = tempFile.name,
                    md5 = client.md5,
                    active = true,
                    size = client.size
                )
            ),
            progressBar = progressBar,
            targetFolder = tempFile.parent
        )
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