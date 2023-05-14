package com.lezenford.mfr.javafx.component

import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.image.Image
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.launch
import com.lezenford.mfr.javafx.extensions.runFx

/**
 * Реализация контроллера может создаваться как вручную, так и с помощью DI-контейнера
 * При вызову в конструкторе super будет автоматически инициализирован fxml контекст
 */
abstract class FxController(
    source: String,
    owner: FxController? = null,
    stageStyle: StageStyle = StageStyle.TRANSPARENT,
    css: String? = CSS
) : FXMLComponent() {
    final override val fxmlLoader: FXMLLoader = FXMLLoader()

    /**
     * Stage обязательно должен создаваться в JavaFx Thread.
     * В данном случае используется специальный CoroutineScope
     */
    protected val stage: Stage = runFx {
        Stage(stageStyle).apply {
            icons.add(Image("icon.png"))
            title = TITLE
            initModality(Modality.APPLICATION_MODAL)
            owner?.also { initOwner(it.stage) }
            onShowing = EventHandler { onShowing() }
            onHidden = EventHandler { onHiding() }
        }
    }

    /**
     * Scene обязательно должна создаваться в JavaFx Thread и строго после Stage
     * В данном случае используется специальный CoroutineScope
     */
    protected val scene: FxScene = runFx {
        stage.javaClass.classLoader.getResourceAsStream(source).use { inputStream ->
            fxmlLoader.setControllerFactory { this@FxController }
            FxScene(root = fxmlLoader.load(inputStream), stage = stage, css = css)
        }
    }

    /**
     * Переопределяемый метод для установки действий, производимых при открытии окна
     */
    protected open fun onShowing() {}

    /**
     * Переопределяемый метод для установки действий, производимых при закрытии окна
     */
    protected open fun onHiding() {}

    /**
     * Сворачивает окно в панель задач
     */
    fun hide() {
        launch {
            close()
        }
    }

    /**
     * Скрывает окно с экрана и отправляет соответствующее уведомление
     */
    fun close() {
        launch { stage.close() }
    }

    /**
     * Отображает окно и блокирует выполнение функции до тех пор, пока окно не будет закрыто.
     * Используется для отображения всплывающих окон
     */
    fun showAndWait() = runFx {
        stage.showAndWait()
    }

    /**
     * Отображает окно не блокируя другие окна
     */
    fun show() {
        launch { stage.show() }
    }

    companion object {
        private const val TITLE = "M[FR]"
        private const val CSS = "javafx/style.css"
    }
}