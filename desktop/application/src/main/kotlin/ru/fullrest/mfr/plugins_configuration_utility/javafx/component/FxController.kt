package ru.fullrest.mfr.plugins_configuration_utility.javafx.component

import javafx.fxml.FXML
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.Window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable
import javax.annotation.PostConstruct
import kotlin.coroutines.CoroutineContext

abstract class FxController : CoroutineScope, Loggable {
    lateinit var stage: Stage
    lateinit var scene: FxScene

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.JavaFx

    /**
     * Инициализация контроллера от JavaFX.
     * Метод вызывается после того как FXML загрузчик произвел инъекции полей.
     * <p>
     * Обратите внимание, что имя метода <b>обязательно</b> должно быть "initialize",
     * в противном случае, метод не вызовется.
     * <p>
     * Также на этом этапе еще отсутствуют бины спринга
     * и для инициализации лучше использовать метод,
     * описанный аннотацией @PostConstruct.
     * Который вызовется спрингом, после того,
     * как им будут произведены все оставшиеся инъекции.
     */
    @FXML
    fun initialize() {
    }

    /**
     * На этом этапе контроллер уже добавлен в контекст спринга
     * и все его бины инициированы.
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
        stage.show()
    }

    fun showAndWait() {
        stage.showAndWait()
    }

    fun hide() {
        stage.hide()
    }

    open fun setOwnerAndModality(stage: Window, modality: Modality) {
        this.stage.initOwner(stage)
        this.stage.initModality(modality)
    }
}