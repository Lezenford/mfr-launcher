package ru.fullrest.mfr.configurator.component.content

import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.TreeItem
import ru.fullrest.mfr.configurator.controller.TextFieldController
import java.nio.file.Path

abstract class ContentTreeItem(
    name: String,
    protected val parent: TreeItem<ContentTreeItem>
) : ContentItem(name) {
    init {
        Button("...").also { button ->
            children.add(button)
            button.setOnMouseClicked {
                TextFieldController(label.text) { newName ->
                    parent.children.find { it.value.name == newName && it.value != this }?.also {
                        Alert(Alert.AlertType.ERROR, "Опция с названием $newName уже существует").showAndWait()
                    } ?: kotlin.run { label.text = newName }
                }.showAndWait()
            }
        }
        Button("-").also { button ->
            children.add(button)
            button.setOnMouseClicked {
                Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Удалить $name?",
                    ButtonType.CANCEL,
                    ButtonType.OK
                ).showAndWait().filter { it.buttonData.isCancelButton.not() }.ifPresent {
                    parent.children.removeIf { item -> item.value == this }
                }
            }
        }
    }
}

class OptionContentTreeItem(
    name: String, parent: TreeItem<ContentTreeItem>,
    private val coreFiles: MutableSet<Path>
) : ContentTreeItem(name, parent) {
    val fileMap: MutableMap<Path, Pair<String, String>> = mutableMapOf()
    var description: String = ""
    var imagePath: Path? = null
        set(value) {
            field?.also { coreFiles.remove(it) }
            value?.also { coreFiles.add(it) }
            field = value
        }

    fun addFiles(files: Collection<Path>) {
        this.files.addAll(files)
        parent.value.files.addAll(files)
    }

    fun removeFiles(files: Collection<Path>) {
        this.files.removeAll(files)
        parent.value.files.addAll(files)
        files.forEach { fileMap.remove(it) }
    }

    fun addFileDetails(path: Path, gamePath: String, storagePath: String) {
        fileMap[path] = gamePath to storagePath
    }
}

class PackageContentTreeItem(
    name: String, parent: TreeItem<ContentTreeItem>,
    coreFiles: MutableSet<Path>
) : ContentTreeItem(name, parent) {
    init {
        Button("+").also { button ->
            children.add(children.size - 1, button)
            button.setOnMouseClicked {
                TextFieldController { name ->
                    parent.children.first { it.value.name == this@PackageContentTreeItem.name }.also { node ->
                        node.children.find { it.value.name == name }?.also {
                            Alert(Alert.AlertType.ERROR, "Опция с названием $name уже существует").showAndWait()
                        } ?: node.children.add(
                            TreeItem(OptionContentTreeItem(name, node, coreFiles))
                        )
                        node.isExpanded = true
                    }
                }.showAndWait()
            }
        }
    }
}
