package ru.fullrest.mfr.configurator.component.tab

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
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.configurator.component.content.ContentItem
import java.nio.file.Path

class FileListTab(
    name: String,
    contentItem: ContentItem,
    updateTree: () -> Unit
) : WorkTab(name, contentItem) {
    private val listView: ListView<ListViewRow> = ListView<ListViewRow>()
    private val fileItems: MutableMap<Path, ListViewRow> = mutableMapOf()
    private val commonCheckBox = CheckBox()
    private val selectedItems: MutableSet<ListViewRow> = mutableSetOf()

    val files: Set<Path>
        get() = fileItems.keys.toSet()

    init {
        isClosable = true
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
                                .map { it.path }
                                .also { removeFiles(it) }
                            updateTree()
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
        addFiles(contentItem.files)
    }

    override fun addFiles(files: Collection<Path>) {
        contentItem.files.addAll(files)
        (files - fileItems.keys).forEach {
            ListViewRow(it).also { row ->
                listView.items.add(row)
                fileItems[it] = row
            }
        }
    }

    override fun removeFiles(files: Collection<Path>) {
        contentItem.files.removeAll(files)
        files.forEach {
            fileItems.remove(it)?.also { row ->
                listView.items.remove(row)
                selectedItems.remove(row)
            }
        }
    }

    private inner class ListViewRow(val path: Path) : HBox() {
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
                        if (listView.items.size - selectedItems.size == 1) {
                            commonCheckBox.isSelected = false
                        }
                    }
                },
                Separator(Orientation.VERTICAL),
                Label().also { field ->
                    field.text = path.toString()
                }
            )
        }
    }

    companion object {
        private val log by Logger()
    }
}