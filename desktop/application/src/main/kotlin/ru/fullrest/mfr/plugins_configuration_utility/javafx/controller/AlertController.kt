package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.RestClientException
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import kotlin.system.exitProcess

class AlertController : FxController() {

    @FXML
    private lateinit var title: Label

    @FXML
    private lateinit var description: TextArea

    @FXML
    private lateinit var closeButton: Button

    @Autowired
    private lateinit var gameInstallController: GameInstallController

    suspend fun error(
        title: String = "",
        exception: Throwable? = null,
        description: String? = null,
        closeButtonEvent: EventHandler<ActionEvent> = EventHandler { exitProcess(0) }
    ) = launch {
        exception?.also { log().error(it) }
        this@AlertController.title.text = title
        this@AlertController.description.text = description ?: let {
            exception?.let {
                when (exception) {
                    is RestClientException -> "Ошибка подключения к серверу"
                    else -> DEFAULT_DESCRIPTION
                }
            } ?: ""
        }
        this@AlertController.closeButton.onAction = closeButtonEvent
        this@AlertController.showAndWait()
    }.join()

    companion object {
        private const val DEFAULT_DESCRIPTION = "Произошла непредвиденная ошибка"
    }
}