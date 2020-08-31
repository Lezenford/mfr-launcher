package ru.fullrest.mfr.plugins_configuration_utility.config

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.fullrest.mfr.plugins_configuration_utility.exception.ApplicationStartException
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxScene
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
    fun gameInstallFXController(): GameInstallController {
        return gameInstallController as GameInstallController
    }

    @Bean
    fun launcherScreenFXController(): LauncherController {
        return launcherController as LauncherController
    }

    @Bean
    fun gameUpdateFXController(): GameUpdateController {
        return gameUpdateController as GameUpdateController
    }

    @Bean
    fun alertScreenFXController(): AlertController {
        return alertController as AlertController
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
        private const val TITLE = "M[FR] Launcher"
        private const val CSS = "javafx/css/style.css"

        lateinit var startController: FxController
        private lateinit var gameInstallController: FxController
        private lateinit var launcherController: FxController
        private lateinit var gameUpdateController: FxController
        private lateinit var alertController: FxController
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
         */
        fun init() {
            log().info("Init JavaFX controllers")
            startController = initController("fxml/start.fxml")
            gameInstallController = initController("fxml/game-install.fxml")
            launcherController = initController("fxml/launcher.fxml")
            gameUpdateController = initController("fxml/game-update.fxml")
            alertController = initController("fxml/alert.fxml")
            helpProjectController = initController("fxml/help-for-project.fxml")
            welcomeController = initController("fxml/welcome_message.fxml")
            pluginConfigurationController = initController("fxml/plugin-configuration.fxml")
            readmeController = initController("fxml/readme.fxml")
            mgeController = initController("fxml/mge-configuration.fxml")
            openMwController = initController("fxml/openmw-configuration.fxml")
            configurationEditorController = initController("fxml/configuration-editor.fxml")
            configurationEditorFieldController = initController("fxml/configuration-editor-field.fxml")
            insertKeyController = initController("fxml/insert-key.fxml")
        }

        private fun initController(uri: String): FxController {
            try {
                return Stage().let { stage ->
                    stage.javaClass.classLoader.getResourceAsStream(uri).use { fxmlStream ->
                        val loader = FXMLLoader()
                        loader.load<Parent>(fxmlStream)
                        val scene = FxScene(loader.getRoot(), Color.TRANSPARENT, stage).also {
                            it.stylesheets.add(CSS)
                        }
                        stage.scene = scene
                        stage.title = TITLE
                        stage.initStyle(StageStyle.TRANSPARENT)
                        stage.icons.add(Image("icon.png"))
                        loader.getController<FxController>().also {
                            it.scene = scene
                            it.stage = stage
                        }
                    }
                }
            } catch (e: Exception) {
                throw ApplicationStartException("Can't init controller for $uri")
            }
        }
    }
}