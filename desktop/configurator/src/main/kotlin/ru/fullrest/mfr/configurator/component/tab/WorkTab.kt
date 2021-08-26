package ru.fullrest.mfr.configurator.component.tab

import javafx.scene.control.Tab
import ru.fullrest.mfr.configurator.component.content.ContentItem
import java.nio.file.Path

abstract class WorkTab(
    name: String,
    open val contentItem: ContentItem
) : Tab(name) {
    abstract fun addFiles(files: Collection<Path>)

    abstract fun removeFiles(files: Collection<Path>)
}