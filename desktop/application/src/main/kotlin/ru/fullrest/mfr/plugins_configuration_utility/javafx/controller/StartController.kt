package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import ru.fullrest.mfr.api.Links
import ru.fullrest.mfr.plugins_configuration_utility.common.FileNameConstant
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationProperties
import ru.fullrest.mfr.plugins_configuration_utility.javafx.TaskFactory
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Properties
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository
import ru.fullrest.mfr.plugins_configuration_utility.service.FileService
import java.io.File
import java.net.URI
import java.util.*
import kotlin.system.exitProcess

class StartController : FxController(), CommandLineRunner {

    @Autowired
    private lateinit var launcherController: LauncherController

    @Autowired
    private lateinit var gameInstallController: GameInstallController

    @Autowired
    private lateinit var gameUpdateController: GameUpdateController

    @Autowired
    private lateinit var welcomeController: WelcomeController

    @Autowired
    private lateinit var files: ApplicationFiles

    @Autowired
    private lateinit var properties: ApplicationProperties

    @Autowired
    private lateinit var propertiesRepository: PropertiesRepository

    @Autowired
    private lateinit var fileService: FileService

    @Autowired
    private lateinit var taskFactory: TaskFactory

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var alertController: AlertController

    @Autowired
    private lateinit var applicationProperties: ApplicationProperties

    private suspend fun startApplication() {
        hide()
        if (propertiesRepository.existsByKey(PropertyKey.INSTANCE_KEY).not()) {
            propertiesRepository.save(Properties(PropertyKey.INSTANCE_KEY, UUID.randomUUID().toString()))
        }
        checkApplicationVersion()
        if (propertiesRepository.existsByKey(PropertyKey.INSTALLED).not()) {
            if (files.checkInstall()) {
                propertiesRepository.save(Properties(PropertyKey.INSTALLED))
            } else {
                gameInstallController.showAndWaitDownloadGame()
            }
        }
        if (propertiesRepository.existsByKey(PropertyKey.INSTALLED)) {
            files.init()
            fileService.prepareOpenMwConfigFiles()
            validateSchema()
            checkFirstStart()
            updateEsmFileChangeDate()
            launcherController.show()
        } else {
            log().error("Game isn't installed")
            exitProcess(0)
        }
    }

    private suspend fun checkApplicationVersion() {
        val betaKey = propertiesRepository.findByKey(PropertyKey.BETA)?.value

        val clientKey = propertiesRepository.findByKey(PropertyKey.INSTANCE_KEY)?.value
            ?: alertController.error(description = "Ошибка запуска приложения")

        val httpEntity = HttpHeaders().also { headers ->
            betaKey?.also { headers.set(HttpHeaders.AUTHORIZATION, "Bearer $betaKey") }
            headers.set(HttpHeaders.COOKIE, "Key=$clientKey")
        }.let { HttpEntity<Unit>(it) }
        val server = betaKey?.let { applicationProperties.testServerLink } ?: applicationProperties.serverLink
        try {
            val version: String = restTemplate.exchange(
                URI.create("$server${Links.LAUNCHER_VERSION}"),
                HttpMethod.GET,
                httpEntity,
                String::class.java
            ).body ?: return
            if (properties.applicationVersion != version) {
                gameInstallController.showAndWaitUpdateLauncher()
            }
        } catch (e: Exception) {
        }
    }

    private suspend fun validateSchema() {
        val schemaProperty = propertiesRepository.findByKey(PropertyKey.SCHEMA) ?: Properties(PropertyKey.SCHEMA)
        if (schemaProperty.value != fileService.getFileMD5(files.schema)?.contentToString()) {
            gameUpdateController.runJob(taskFactory.getFillSchemeTask())
            gameUpdateController.showAndWait()
        }
    }

    private fun updateEsmFileChangeDate() {
        FileNameConstant.esmFileList.forEach {
            try {
                File("${files.dataFiles.absolutePath}${File.separator}${it.first}").setLastModified(it.second)
            } catch (e: Exception) {
                log().error("Error to change last modified date in file ${it.first}", e)
            }
        }
    }

    private fun checkFirstStart() {
        propertiesRepository.findByKey(PropertyKey.FIRST_START) ?: run {
            propertiesRepository.save(Properties(key = PropertyKey.FIRST_START))
            fileService.setOpenMwConfig(files.middlePerformanceOpenMwFolder)
            welcomeController.showAndWait()
        }
    }

    override fun run(vararg args: String?) {
        launch {
            startApplication()
        }
    }
}