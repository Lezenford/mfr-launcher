package ru.fullrest.mfr.configurator.component.content

import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.control.Alert
import javafx.scene.control.ListView
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.MouseButton
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.javafx.component.FXMLComponent
import java.nio.file.Path

class ContentPane(override val fxmlLoader: FXMLLoader) : FXMLComponent() {
    val mainContentView: ListView<ContentListItem> by fxml()
    val extraContentView: ListView<ContentListItem> by fxml()
    val optionContentTree: TreeView<ContentTreeItem> by fxml()

    init {
        val element = ContentListItem(CORE_CONTENT_ITEM, mainContentView, false).apply {
            prefHeight = 35.0
            minHeight = 35.0
            maxHeight = 35.0
        }
        mainContentView.apply {
            prefHeight = 35.0
            minHeight = 35.0
            maxHeight = 35.0
            fixedCellSize = 35.0
        }
        mainContentView.items.add(element)
        optionContentTree.root = TreeItem()
        optionContentTree.onMouseClicked = EventHandler {
            if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                optionContentTree.selectionModel.selectedItem.also { selectedItem ->
                    if (selectedItem.children.isEmpty()) {

                    }
                }
            }
        }
    }

    fun addExtraContentItem(name: String) {
        extraContentView.items.find { it.name == name }?.also {
            Alert(Alert.AlertType.ERROR, "Опция с названием $name уже существует").showAndWait()
        } ?: extraContentView.items.add(ContentListItem(name, extraContentView))
    }

    fun addOptionalContentPackage(name: String) {
        optionContentTree.root.children.find { it.value.name == name }?.also {
            Alert(Alert.AlertType.ERROR, "Опция с названием $name уже существует").showAndWait()
        } ?: optionContentTree.root.children.add(
            TreeItem(
                PackageContentTreeItem(
                    name,
                    optionContentTree.root,
                    mainContentView.items.first().files
                )
            )
        )
    }

    fun allFiles(): Set<Path> {
        return (mainContentView.items.flatMap { it.files } +
                extraContentView.items.flatMap { it.files } +
                optionContentTree.root.children.flatMap { it.value.files }).toSet()
    }

    fun removeFiles(files: List<Path>) {
        mainContentView.items.forEach { it.files.removeAll(files) }
        extraContentView.items.forEach { it.files.removeAll(files) }
        optionContentTree.root.children.flatMap { it.children }.map { it.value }
            .filterIsInstance<OptionContentTreeItem>().forEach { it.removeFiles(files) }
    }

    companion object {
        private val log by Logger()
        private const val CORE_CONTENT_ITEM = "Core"
    }
}