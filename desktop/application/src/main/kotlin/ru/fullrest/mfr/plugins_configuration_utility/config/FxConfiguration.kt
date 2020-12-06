package ru.fullrest.mfr.plugins_configuration_utility.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.fullrest.mfr.plugins_configuration_utility.exception.StartApplicationException
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.initController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.*
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable

@Configuration
class FxConfiguration {

    /**
     * Add controllers to context
     */
    @Bean
    fun startScreenFXController(): StartController {
        return startController as StartController
    }

    @Bean
    fun gameInstallFXController(): GlobalProgressController {
        return gameInstallController as GlobalProgressController
    }

    @Bean
    fun launcherScreenFXController(): LauncherController {
        return launcherController as LauncherController
    }

    @Bean
    fun helpForProjectController(): HelpForProjectController {
        return helpProjectController as HelpForProjectController
    }

    @Bean
    fun welcomeController(): WelcomeController {
        return welcomeController as WelcomeController
    }

    @Bean
    fun pluginConfigurationController(): PluginConfigurationController {
        return pluginConfigurationController as PluginConfigurationController
    }

    @Bean
    fun readmeController(): ReadmeController {
        return readmeController as ReadmeController
    }

    @Bean
    fun mgeConfigurationController(): MgeConfigurationController {
        return mgeController as MgeConfigurationController
    }

    @Bean
    fun openMwConfigurationController(): OpenMwConfigurationController {
        return openMwController as OpenMwConfigurationController
    }

    @Bean
    fun configurationEditorController(): ConfigurationEditorController {
        return configurationEditorController as ConfigurationEditorController
    }

    @Bean
    fun configurationEditorFieldController(): ConfigurationEditorFieldController {
        return configurationEditorFieldController as ConfigurationEditorFieldController
    }

    @Bean
    fun insertKeyController(): InsertKeyController {
        return insertKeyController as InsertKeyController
    }

    companion object : Loggable {
        private var initialized: Boolean = false

        private lateinit var startController: FxController
        private lateinit var gameInstallController: FxController
        private lateinit var launcherController: FxController
        private lateinit var helpProjectController: FxController
        private lateinit var welcomeController: FxController
        private lateinit var pluginConfigurationController: FxController
        private lateinit var readmeController: FxController
        private lateinit var mgeController: FxController
        private lateinit var openMwController: FxController
        private lateinit var configurationEditorController: FxController
        private lateinit var configurationEditorFieldController: FxController
        private lateinit var insertKeyController: FxController

        /**
         * Must be invoke in JavaFX thread only
         * Can't create stages and controllers from other threads
         * Show first screen before closed
         */
        fun init() {
            if (initialized) {
                throw StartApplicationException("JavaFx already initialized")
            }
            log().info("Init JavaFX controllers")

            startController = initController("fxml/start.fxml")
            gameInstallController = initController("fxml/game-install.fxml")
            launcherController = initController("fxml/launcher.fxml")
            helpProjectController = initController("fxml/help-for-project.fxml", launcherController.stage)
            welcomeController = initController("fxml/welcome_message.fxml", launcherController.stage)
            pluginConfigurationController = initController("fxml/plugin-configuration.fxml")
            readmeController = initController("fxml/readme.fxml", launcherController.stage)
            mgeController = initController("fxml/mge-configuration.fxml", launcherController.stage)
            openMwController = initController("fxml/openmw-configuration.fxml", launcherController.stage)
            configurationEditorController = initController("fxml/configuration-editor.fxml")
            configurationEditorFieldController =
                initController("fxml/configuration-editor-field.fxml", configurationEditorController.stage)
            insertKeyController = initController("fxml/insert-key.fxml")

            startController.show()
            initialized = true
        }
    }
}