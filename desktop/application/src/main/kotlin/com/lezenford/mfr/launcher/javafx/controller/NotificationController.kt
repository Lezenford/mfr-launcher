package com.lezenford.mfr.launcher.javafx.controller

import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.stage.Modality
import javafx.stage.Stage
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Profile
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import com.lezenford.mfr.javafx.component.FxController
import com.lezenford.mfr.javafx.extensions.runFx
import kotlin.system.exitProcess

/**
 * Данный контроллер может вызываться из разных мест UI и каждый раз требует
 * создания нового экземпляра, т.к. Stage должен иметь различных owner.
 */
@Profile("GUI")
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class NotificationController : FxController("fxml/notification.fxml") {
    private val title: Label by fxml()
    private val description: TextArea by fxml()
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
            this.stage.show()
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
            this.stage.show()
        }
    }
}