package ru.fullrest.mfr.configurator.component.content

import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import java.nio.file.Path

abstract class ContentItem(name: String) : HBox() {
    val files: MutableSet<Path> = mutableSetOf()
    protected val label: Label =
        Label(name).also { children.addAll(it, HBox().apply { setHgrow(this, Priority.ALWAYS) }) }
    val name: String
        get() = label.text
}