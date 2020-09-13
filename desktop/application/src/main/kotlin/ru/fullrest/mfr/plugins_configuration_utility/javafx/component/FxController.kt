package ru.fullrest.mfr.plugins_configuration_utility.javafx.component

import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import ru.fullrest.mfr.plugins_configuration_utility.exception.StartApplicationException
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable
import javax.annotation.PostConstruct
import kotlin.coroutines.CoroutineContext

private const val TITLE = "M[FR] Launcher"
private const val CSS = "javafx/css/style.css"

fun initController(uri: String, owner: Window? = null, modality: Modality = Modality.APPLICATION_MODAL): FxController {
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
                stage.initModality(modality)
                owner?.also { stage.initOwner(owner) }
                loader.getController<FxController>().also { controller ->
                    controller.scene = scene
                    controller.stage = stage
                }
            }
        }
    } catch (e: Exception) {
        throw StartApplicationException("Can't init controller for $uri")
    }
}

abstract class FxController : CoroutineScope, Loggable {
    lateinit var stage: Stage
    lateinit var scene: FxScene

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.JavaFx

    /**
     * На этом этапе контроллер уже добавлен в контекст спринга
     * и все его бины инициализированы.
     * Данный метод можно безопасно использовать для дополнительной
     * инициации контроллеров, например добавления listener'ов на FXML
     * объекты или их наполнением данными
     */
    @PostConstruct
    protected open fun init() {
    }

    val showing
        get() = stage.isShowing

    fun show() {
        launch { stage.show() }
    }

    fun showAndWait() {
        stage.showAndWait()
    }

    fun hide() {
        launch { stage.hide() }
    }
}