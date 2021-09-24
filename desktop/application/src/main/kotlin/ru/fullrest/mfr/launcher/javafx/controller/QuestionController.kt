package ru.fullrest.mfr.launcher.javafx.controller

import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.stage.Modality
import javafx.stage.Stage
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.javafx.component.FxController
import ru.fullrest.mfr.javafx.extensions.runFx
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Данный контроллер может вызываться из разных мест UI и каждый раз требует
 * создания нового экземпляра, т.к. Stage должен иметь различных owner.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class QuestionController : FxController("fxml/question.fxml") {
    private val title: Label by fxml()
    private val description: TextArea by fxml()
    private val okButton: Button by fxml()
    private val cancelButton: Button by fxml()

    init {
        stage.initModality(Modality.WINDOW_MODAL)
        stage.initOwner(Stage.getWindows().find { it.isShowing })
    }

    fun show(
        title: String = "Внимание",
        description: String
    ): Boolean = runFx {
        this.title.text = title
        this.description.text = description
        val result = AtomicBoolean()
        okButton.onMouseClicked = EventHandler {
            result.set(true)
            hide()
        }
        cancelButton.onMouseClicked = EventHandler {
            result.set(false)
            hide()
        }
        showAndWait()
        return@runFx result.get()
    }
}