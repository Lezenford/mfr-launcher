package ru.fullrest.mfr.launcher.javafx.controller

import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.stage.Modality
import javafx.stage.Stage
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.javafx.component.FxController
import ru.fullrest.mfr.javafx.extensions.runFx
import kotlin.system.exitProcess

/**
 * Данный контроллер может вызываться из разных мест UI и каждый раз требует
 * создания нового экземпляра, т.к. Stage должен иметь различных owner.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class NotificationController : FxController("fxml/notification.fxml") {
    private val title: Label by fxml()
    private val description: Label by fxml()
    private val closeButton: Button by fxml()

    init {
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(Stage.getWindows().find { it.isShowing })
    }

    fun error(
        title: String = "Ошибка",
        description: String
    ) {
        runFx {
            this.title.text = title
            this.description.text = description
            this.closeButton.onAction = EventHandler { exitProcess(0) }
            this.stage.showAndWait()
        }
    }

    fun info(
        title: String = "Внимание",
        description: String
    ) {
        runFx {
            this.title.text = title
            this.description.text = description
            this.closeButton.onAction = EventHandler { hide() }
            this.stage.showAndWait()
        }
    }
}