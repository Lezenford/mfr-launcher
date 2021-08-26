package ru.fullrest.mfr.javafx.component

import javafx.fxml.FXMLLoader
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class FXMLComponent {
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