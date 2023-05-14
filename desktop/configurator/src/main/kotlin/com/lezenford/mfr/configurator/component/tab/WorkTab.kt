package com.lezenford.mfr.configurator.component.tab

import com.lezenford.mfr.configurator.component.ContentViewItem
import com.lezenford.mfr.configurator.content.GameFile
import javafx.beans.property.ObjectPropertyBase
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.control.Tab

abstract class WorkTab(
    name: String
) : Tab(name) {
    val contentChangeObserver: ChangeObserver = ChangeObserver()
    abstract val contentViewItem: ContentViewItem<*>
    abstract fun addFiles(files: Set<GameFile>)
    abstract fun removeFiles(files: Set<GameFile>)
}