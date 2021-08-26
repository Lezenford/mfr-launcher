package ru.fullrest.mfr.configurator.extensions

import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import ru.fullrest.mfr.configurator.component.FileItem
import ru.fullrest.mfr.configurator.component.FileTree
import ru.fullrest.mfr.configurator.component.FolderItem
import ru.fullrest.mfr.configurator.component.PathItem
import java.nio.file.Path

fun <T> TreeItem<T>.listAllFiles(): List<T> {
    return if (children.isEmpty()) {
        listOf(value)
    } else {
        children.flatMap { it.listAllFiles() }
    }
}

fun <T : PathItem> TreeItem<T>.walkAndBoldExisting(items: Set<Path>) {
    children.sortedBy { it.value is FolderItem }.forEach { it.walkAndBoldExisting(items) }
    when (value) {
        is FileItem -> if (items.contains(value.path)) value.style = "-fx-font-weight: bold" else value.style = ""
        is FolderItem -> if (children.all { it.value.style.isNotBlank() }) {
            value.style = "-fx-font-weight: bold"
        } else {
            value.style = ""
        }
    }
}

fun TreeView<PathItem>.correct(fileTree: FileTree, filter: Set<Path>) {
    val expanded = mutableSetOf<Path>()
    root.children.forEach { item ->
        item.recursive {
            if (isExpanded) {
                expanded.add(value.path)
            }
        }
    }
    root = fileTree.toTreeItem(filter)
    root.children.forEach { it.recursive { isExpanded = expanded.contains(value.path) } }
}

private fun <T> TreeItem<T>.recursive(function: TreeItem<T>.() -> Unit) {
    function()
    children.forEach { it.recursive(function) }
}