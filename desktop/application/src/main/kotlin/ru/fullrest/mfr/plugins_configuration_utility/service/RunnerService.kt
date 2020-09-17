package ru.fullrest.mfr.plugins_configuration_utility.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service
import ru.fullrest.mfr.api.Links
import ru.fullrest.mfr.plugins_configuration_utility.common.FileNameConstant
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.exception.ExceptionHandler
import ru.fullrest.mfr.plugins_configuration_utility.javafx.TaskFactory
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.GlobalProgressController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.LauncherController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.StartController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.WelcomeController
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Properties
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository
import java.io.File
import java.util.*
import kotlin.system.exitProcess

@Service
class RunnerService(
    private val launcherController: LauncherController,
    private val globalProgressController: GlobalProgressController,
    private val welcomeController: WelcomeController,
    private val startController: StartController,
    private val files: ApplicationFiles,
    private val properties: ApplicationProperties,
    private val propertiesRepository: PropertiesRepository,
    private val fileService: FileService,
    private val taskFactory: TaskFactory,
    private val restTemplateService: RestTemplateService,
    private val exceptionHandler: ExceptionHandler
) : CommandLineRunner, Loggable {
    override fun run(vararg args: String?) {
        CoroutineScope(Dispatchers.Default).launch {
            CoroutineScope(Dispatchers.JavaFx).launch {
                Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
            }

            if (propertiesRepository.existsByKey(PropertyKey.INSTANCE_KEY).not()) {
                propertiesRepository.save(Properties(PropertyKey.INSTANCE_KEY, UUID.randomUUID().toString()))
            }
            startController.hide()
            checkApplicationVersion()
            if (propertiesRepository.existsByKey(PropertyKey.INSTALLED).not()) {
                if (files.checkInstall()) {
                    propertiesRepository.save(Properties(PropertyKey.INSTALLED))
                } else {
                    withContext(Dispatchers.JavaFx) {
                        globalProgressController.showAndWaitDownloadGame()
                    }
                }
            }
            if (propertiesRepository.existsByKey(PropertyKey.INSTALLED)) {
                files.init()
                fileService.prepareOpenMwConfigFiles()
                validateSchema()
                updateEsmFileChangeDate()
                checkFirstStart()
                launcherController.show()
            } else {
                log().error("Game isn't installed")
                exitProcess(0)
            }
        }
    }

    private suspend fun checkApplicationVersion() {
        try {
            val version: String = restTemplateService.exchange(
                link = Links.LAUNCHER_VERSION,
                clazz = String::class.java
            ) ?: return
            if (properties.applicationVersion != version) {
                withContext(Dispatchers.JavaFx) { globalProgressController.showAndWaitUpdateLauncher() }
            }
        } catch (e: Exception) {
            log().error(e)
        }
    }

    private suspend fun validateSchema() {
        val schemaProperty = propertiesRepository.findByKey(PropertyKey.SCHEMA) ?: Properties(PropertyKey.SCHEMA)
        if (schemaProperty.value != fileService.getFileMD5(files.schema)?.contentToString()) {
            withContext(Dispatchers.JavaFx) {
                taskFactory.getFillSchemeTask().run()
            }
        }
    }

    private suspend fun updateEsmFileChangeDate() {
        FileNameConstant.esmFileList.forEach {
            try {
                File("${files.dataFiles.absolutePath}${File.separator}${it.first}").setLastModified(it.second)
            } catch (e: Exception) {
                log().error("Error to change last modified date in file ${it.first}", e)
            }
        }
    }

    private suspend fun checkFirstStart() {
        propertiesRepository.findByKey(PropertyKey.FIRST_START) ?: run {
            propertiesRepository.save(Properties(key = PropertyKey.FIRST_START))
            fileService.setOpenMwConfig(files.middlePerformanceOpenMwFolder)
            withContext(Dispatchers.JavaFx) { welcomeController.showAndWait() }
        }
    }
}