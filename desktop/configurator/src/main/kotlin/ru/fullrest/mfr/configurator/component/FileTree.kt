package ru.fullrest.mfr.configurator.component

import javafx.scene.control.TreeItem
import java.nio.file.Path
import java.util.stream.Collectors

class FileTree(
    private val root: Node,
) {
    class Node(
        val path: Path,
        val children: List<Node> = emptyList()
    ) {
        fun toTreeItem(filter: Set<Path>): TreeItem<PathItem>? {
            return if (children.isNotEmpty()) {
                children.mapNotNull { it.toTreeItem(filter) }.takeIf { it.isNotEmpty() }?.let {
                    TreeItem<PathItem>(FolderItem(path)).apply {
                        children.addAll(it.sortedBy { it.value.path }.sortedBy { it.children.isEmpty() })
                    }
                }
            } else {
                path.takeIf { filter.contains(it) }?.let { TreeItem(FileItem(it)) }
            }
        }
    }

    fun toTreeItem(filter: Set<Path> = allFiles()): TreeItem<PathItem> =
        root.toTreeItem(filter) ?: TreeItem<PathItem>(FolderItem(root.path))

    //FIXME убрать параллельные потоки и оптимизировать скорость выборки
    fun invertFilter(filter: Set<Path>): Set<Path> {
        return allFiles().parallelStream().filter { file -> filter.none { file.startsWith(it) } }
            .collect(Collectors.toSet())
    }

    private fun allFiles(): Set<Path> {
        val all = mutableListOf<Path>()
        root.recursive { all.add(path) }
        return all.toSet()
    }

    private fun Node.recursive(function: Node.() -> Unit) {
        function()
        children.forEach { it.recursive(function) }
    }
}