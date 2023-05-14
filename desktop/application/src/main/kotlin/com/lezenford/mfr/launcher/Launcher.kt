package com.lezenford.mfr.launcher

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.launcher.config.FxConfiguration
import com.lezenford.mfr.launcher.exception.ServerConnectionException
import com.lezenford.mfr.launcher.exception.StartApplicationException
import com.lezenford.mfr.launcher.service.initiator.InitApplicationInitiator
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.io.File
import java.util.*
import kotlin.system.exitProcess

@SpringBootConfiguration
@ImportAutoConfiguration(
    classes = [AopAutoConfiguration::class,
        DataSourceAutoConfiguration::class,
        DataSourceTransactionManagerAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
        JpaRepositoriesAutoConfiguration::class,
        PersistenceExceptionTranslationAutoConfiguration::class,
        LiquibaseAutoConfiguration::class]
)
@EnableTransactionManagement
@EntityScan(basePackages = ["com.lezenford.mfr.launcher.model.entity"])
@EnableJpaRepositories(basePackages = ["com.lezenford.mfr.launcher.model.repository"])
@ComponentScan
@ConfigurationPropertiesScan
@EnableScheduling
class Launcher : Application() {

    override fun start(stage: Stage) {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            log.error("Application take an error and will be closed", e)
            exitProcess(0)
        }
        Platform.setImplicitExit(false)
        FxConfiguration.startController.show()
        initSpringBoot()
    }

    private fun initSpringBoot() = CoroutineScope(Dispatchers.Default).launch {
        try {
            log.info("Start Spring Boot init")
            runApplication<Launcher>(*parameters.raw.toTypedArray() + initExternalConfiguration()).also {
                it.registerShutdownHook()
                it.getBean(InitApplicationInitiator::class.java).init
            }
            log.info("Spring Boot initialization completed")
        } catch (e: ServerConnectionException) {
            throw e
        } catch (e: Exception) {
            throw StartApplicationException("Can't initialize Spring Boot", e)
        }
    }

    private fun initExternalConfiguration(): List<String> {
        return File(CONFIGURATION_FILE).let { file ->
            val lines = file.takeIf { it.exists() }?.readLines() ?: emptyList()
            lines.filter { it.isNotEmpty() }.map { it.split("=") }
                .filter { it.size == 2 }
                .associate { it.first() to it.last() }.toMutableMap()
                .also { it.getOrPut(CLIENT_ID) { UUID.randomUUID().toString() } }
                .also { map ->
                    file.writeBytes(map.map { "${it.key}=${it.value}" }.joinToString("\n").toByteArray())
                }.map { "--${it.key}=${it.value}" }
        }
    }

    companion object {
        private val log by Logger()
        private const val CONFIGURATION_FILE = "launcher.ini"
        private const val CLIENT_ID = "application.clientId"
    }
}

fun main(args: Array<String>) {
    System.setProperty("liquibase.ext.nochangeloglock.enabled", "true")
    Application.launch(Launcher::class.java, *args)
}