package com.lezenford.mfr.javafx.component

import javafx.fxml.FXMLLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlin.coroutines.CoroutineContext
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class FXMLComponent : CoroutineScope {
    /**
     * По-умолчанию все действия в контроллере должны производится в JavaFX Thread
     * В данном случае используется специальный CoroutineScope
     */
    final override val coroutineContext: CoroutineContext = Dispatchers.JavaFx

    protected abstract val fxmlLoader: FXMLLoader

    /**
     * Делегат для инициализации FXML элементов
     * Заменяет собой аннотацию @FXML
     */
    @Suppress("UNCHECKED_CAST")
    protected fun <T : Any> fxml(propName: String? = null): ReadOnlyProperty<FXMLComponent, T> =
        object : ReadOnlyProperty<FXMLComponent, T> {
            private var value: T? = null

            override fun getValue(thisRef: FXMLComponent, property: KProperty<*>): T =
                value ?: setValue(thisRef, property)

            private fun setValue(thisRef: FXMLComponent, property: KProperty<*>): T {
                val key = propName ?: property.name
                return thisRef.fxmlLoader.namespace[key]?.let { it as T }?.also { value = it }
                    ?: throw IllegalArgumentException("Property \"$key\" does not match fx:id declaration")
            }
        }
}