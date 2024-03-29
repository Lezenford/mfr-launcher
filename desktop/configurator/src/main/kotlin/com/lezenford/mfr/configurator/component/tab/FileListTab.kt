package com.lezenford.mfr.configurator.component.tab

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.configurator.component.ContentViewItem
import com.lezenford.mfr.configurator.content.GameFile
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane
import javafx.scene.control.Separator
import javafx.scene.control.ToolBar
import javafx.scene.input.MouseButton
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox

class FileListTab(
    override val contentViewItem: ContentViewItem<*>
) : WorkTab(contentViewItem.name) {
    private val listView: ListView<ListViewRow> = ListView<ListViewRow>()
    private val fileItems: MutableMap<GameFile, ListViewRow> = hashMapOf()
    private val commonCheckBox = CheckBox()
    private val selectedItems: MutableSet<ListViewRow> = mutableSetOf()

    init {
        isClosable = true
        contentViewItem.content.name.addListener { _, _, newValue -> text = newValue }
        contentViewItem.removeButton.addEventHandler(ActionEvent.ACTION) { tabPane.tabs.remove(this) }

        content = VBox().apply {
            VBox.setVgrow(this, Priority.ALWAYS)
            children.addAll(
                ToolBar(
                    commonCheckBox.apply {
                        onMouseClicked = EventHandler {
                            val isSelected = commonCheckBox.isSelected
                            listView.items.forEach { it.checkBox.isSelected = isSelected }
                        }
                    },
                    Separator(Orientation.VERTICAL),
                    Button("Удалить").apply {
                        onMouseClicked = EventHandler {
                            listView.items
                                .filter { it.checkBox.isSelected }
                                .mapTo(hashSetOf()) { it.gameFile }
                                .also { removeFiles(it) }
                            contentChangeObserver.update()
                            commonCheckBox.isSelected = false
                        }
                    },
                ),
                ScrollPane().apply {
                    isFitToHeight = true
                    isFitToWidth = true
                    VBox.setVgrow(this, Priority.ALWAYS)
                    content = listView.also {
                        VBox.setVgrow(it, Priority.ALWAYS)
                    }
                }

            )
        }
        addFiles(contentViewItem.content.files)
    }

    override fun addFiles(files: Set<GameFile>) {
        contentViewItem.content.files.addAll(files)
        (files - fileItems.keys).forEach {
            ListViewRow(it).also { row ->
                listView.items.add(row)
                fileItems[it] = row
            }
        }
        contentChangeObserver.update()
    }

    override fun removeFiles(files: Set<GameFile>) {
        contentViewItem.content.files.removeAll(files)
        files.forEach {
            fileItems.remove(it)?.also { row ->
                listView.items.remove(row)
                selectedItems.remove(row)
            }
        }
        contentChangeObserver.update()
    }

    private inner class ListViewRow(val gameFile: GameFile) : HBox() {
        val checkBox = CheckBox()

        init {
            setHgrow(this, Priority.ALWAYS)
            children.addAll(
                checkBox.also { checkBox ->
                    this.onMouseClicked = EventHandler { event ->
                        if (event.button == MouseButton.PRIMARY && event.clickCount == 1) {
                            checkBox.isSelected = checkBox.isSelected.not()
                        }
                    }
                    checkBox.selectedProperty().addListener { _, _, newValue ->
                        if (newValue) {
                            selectedItems.add(this)
                        } else {
                            selectedItems.remove(this)
                        }
                        if (selectedItems.size == listView.items.size) {
                            commonCheckBox.isSelected = true
                        }
                        if (listView.items.size != selectedItems.size) {
                            commonCheckBox.isSelected = false
                        }
                    }
                },
                Separator(Orientation.VERTICAL),
                Label().also { field ->
                    field.text = gameFile.relativePath.toString()
                }
            )
        }
    }

    companion object {
        private val log by Logger()
    }
}