package ru.fullrest.mfr.launcher.service

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.launcher.component.ApplicationStatus
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.config.properties.GameProperties
import ru.fullrest.mfr.launcher.event.ShowStageRequestEvent
import ru.fullrest.mfr.launcher.exception.ExceptionHandler
import ru.fullrest.mfr.launcher.javafx.controller.LauncherController
import ru.fullrest.mfr.launcher.javafx.controller.OpenMwController
import ru.fullrest.mfr.launcher.javafx.controller.StartController
import ru.fullrest.mfr.launcher.javafx.controller.WelcomeController
import ru.fullrest.mfr.launcher.model.entity.Properties
import ru.fullrest.mfr.launcher.util.FileNameConstant
import kotlin.io.path.exists

@Service
class RunnerService(
    private val startController: StartController,
    private val propertyService: PropertiesService,
    private val applicationProperties: ApplicationProperties,
    private val gameProperties: GameProperties,
    private val eventPublisher: ApplicationEventPublisher,
    private val applicationStatus: ApplicationStatus,
    private val openMwController: OpenMwController,
    private val restTemplateService: RestTemplateService,
    private val exceptionHandler: ExceptionHandler
) {

    @EventListener(ApplicationReadyEvent::class)
    fun run() {
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)

        applicationStatus.gameInstalled.addListener { installed ->
            if (installed) {
                FileNameConstant.esmFileList.forEach { (fileName, modifiedDate) ->
                    applicationProperties.gameFolder.resolve("Data Files").resolve(fileName).takeIf { it.exists() }
                        ?.also { it.toFile().setLastModified(modifiedDate) }
                }
            }
        }
        kotlin.runCatching {
            val buildsDto = restTemplateService.builds()

            val build = propertyService.findByKey(Properties.Key.SELECTED_BUILD) ?: buildsDto.first().run {
                Properties(key = Properties.Key.SELECTED_BUILD, value = name).also { propertyService.save(it) }
            }

            applicationStatus.gameBuildActive.value = buildsDto.first { it.name == build.value }.id
        }.onFailure {
            exceptionHandler.uncaughtException(Thread.currentThread(), it)
        }

        applicationStatus.gameInstalled.value = checkInstall()

        if (applicationStatus.gameInstalled.value) {
            openMwController.prepareTemplates()
        }

        startController.close()
        eventPublisher.publishEvent(ShowStageRequestEvent(this, LauncherController::class))
        propertyService.findByKey(Properties.Key.FIRST_START) ?: kotlin.run {
            eventPublisher.publishEvent(ShowStageRequestEvent(this, WelcomeController::class))
            propertyService.save(Properties(key = Properties.Key.FIRST_START))
        }
    }

    private fun checkInstall(): Boolean {
        return gameProperties.classic.exists() && gameProperties.openMw.exists()
    }

    companion object {
        private val log by Logger()
    }
}