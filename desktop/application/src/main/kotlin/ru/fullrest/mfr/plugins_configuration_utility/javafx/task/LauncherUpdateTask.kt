package ru.fullrest.mfr.plugins_configuration_utility.javafx.task

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.api.Links
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.exception.ExternalApplicationException
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxTask
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.GlobalProgressController
import ru.fullrest.mfr.plugins_configuration_utility.service.RestTemplateService
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class LauncherUpdateTask(
    restTemplateService: RestTemplateService,
    applicationProperties: ApplicationProperties,
    globalProgressController: GlobalProgressController
) : FxTask<Boolean, GlobalProgressController>(restTemplateService, applicationProperties, globalProgressController) {

    override suspend fun process(): Boolean {
        progressController.updateProgress(0, 0)
        progressController.setDescription("Подготовка к скачиванию")

        downloadFile(Links.LAUNCHER_DOWNLOAD)
        val updater = downloadFile(Links.LAUNCHER_UPDATER_DOWNLOAD)

        val file = File("update").also {
            withContext(Dispatchers.IO) {
                try {
                    Files.move(updater.toPath(), it.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } catch (e: Exception) {
                    throw ExternalApplicationException("Ошибка обновления.\nОбновление отменено", e)
                }
            }
        }
        withContext(Dispatchers.IO) {
            Runtime.getRuntime().exec(
                "\"${File("jdk/bin/java.exe").absoluteFile}\" -jar \"${file.absoluteFile}\"",
                null,
                File("").absoluteFile
            )
        }
        exitProcess(0)
    }
}