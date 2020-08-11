package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.VBox
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.client.RestTemplate
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Properties
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.PropertyKey
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.PropertiesRepository

class InsertKeyController : FxController() {

    @Value("\${application.test_server_link}")
    private lateinit var serverLink: String;

    @Autowired
    private lateinit var restTemplate: RestTemplate

    @Autowired
    private lateinit var propertiesRepository: PropertiesRepository

    @FXML
    private lateinit var field: TextField

    @FXML
    private lateinit var acceptButton: VBox

    override fun init() {
        stage.onShowing = EventHandler {
            field.text = ""
        }
        field.textProperty().addListener { _, _, newValue: String? ->
            newValue?.also {
                acceptButton.isDisable = newValue.isBlank()
            }
        }
        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event: KeyEvent ->
            if (event.code == KeyCode.ESCAPE) {
                Platform.exit()
            }
            if (event.code == KeyCode.ENTER && acceptButton.isDisable.not()) {
                accept()
            }
        }
    }

    fun accept() {
        val key = field.text
        if (key.isNotBlank()) {
            val token = restTemplate.getForObject(
                "$serverLink$authApiLink?key=$key",
                String::class.java
            )
            propertiesRepository.save(Properties(PropertyKey.BETA, token))
            hide()
        }
    }

    companion object {
        private const val authApiLink = "auth"
    }
}