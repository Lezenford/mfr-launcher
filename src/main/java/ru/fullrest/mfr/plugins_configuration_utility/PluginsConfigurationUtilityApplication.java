package ru.fullrest.mfr.plugins_configuration_utility;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created on 02.11.2018
 *
 * @author Alexey Plekhanov
 */
@SpringBootConfiguration
@ImportAutoConfiguration(classes = {
        AopAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        JtaAutoConfiguration.class,
        PersistenceExceptionTranslationAutoConfiguration.class,
})
@EnableJpaRepositories(basePackages = "ru.fullrest.mfr.plugins_configuration_utility.model.repository")
@EntityScan(basePackages = "ru.fullrest.mfr.plugins_configuration_utility.model.entity")
@ComponentScan(value = {"ru.fullrest.mfr.plugins_configuration_utility.config",
        "ru.fullrest.mfr.plugins_configuration_utility.javafx",
        "ru.fullrest.mfr.plugins_configuration_utility.manager",
        "ru.fullrest.mfr.plugins_configuration_utility.model.service"},
        basePackageClasses = PluginsConfigurationUtilityApplication.class)
@EnableTransactionManagement
public class PluginsConfigurationUtilityApplication extends AbstractJavaFxApplicationSupport {

    public static void main(String[] args) {
        launchApp(args);
    }
}
