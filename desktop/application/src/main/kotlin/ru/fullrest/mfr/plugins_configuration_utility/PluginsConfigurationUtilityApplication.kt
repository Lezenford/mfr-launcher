package ru.fullrest.mfr.plugins_configuration_utility

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
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import ru.fullrest.mfr.plugins_configuration_utility.config.FxConfiguration
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable
import kotlin.system.exitProcess

@SpringBootConfiguration
@ImportAutoConfiguration(
    classes = [AopAutoConfiguration::class,
        DataSourceAutoConfiguration::class,
        DataSourceTransactionManagerAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
        JpaRepositoriesAutoConfiguration::class,
        JtaAutoConfiguration::class,
        PersistenceExceptionTranslationAutoConfiguration::class,
        LiquibaseAutoConfiguration::class]
)
@EntityScan(basePackages = ["ru.fullrest.mfr.plugins_configuration_utility.model.entity"])
@EnableJpaRepositories(basePackages = ["ru.fullrest.mfr.plugins_configuration_utility.model.repository"])
@ComponentScan(
    value = ["ru.fullrest.mfr.plugins_configuration_utility.config",
        "ru.fullrest.mfr.plugins_configuration_utility.javafx",
        "ru.fullrest.mfr.plugins_configuration_utility.util",
        "ru.fullrest.mfr.plugins_configuration_utility.service"],
    basePackageClasses = [PluginsConfigurationUtilityApplication::class]
)
@EnableTransactionManagement
class PluginsConfigurationUtilityApplication : Application(), Loggable {

    override fun start(primaryStage: Stage) {
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            log().error("Application take an error and will be closed", throwable)
            exitProcess(0)
        }
        Platform.setImplicitExit(false)
        FxConfiguration.init()
        initSpringBoot()
    }

    private fun initSpringBoot() = CoroutineScope(Dispatchers.Default).launch {
        try {
            log().info("Start Spring Boot init")
            runApplication<PluginsConfigurationUtilityApplication>(*parameters.raw.toTypedArray()).registerShutdownHook()
            log().info("Spring Boot initialization completed")
        } catch (e: Exception) {
            log().error(e)
            exitProcess(0)
        }
    }
}

fun main(args: Array<String>) {
    Application.launch(PluginsConfigurationUtilityApplication::class.java, *args)
}