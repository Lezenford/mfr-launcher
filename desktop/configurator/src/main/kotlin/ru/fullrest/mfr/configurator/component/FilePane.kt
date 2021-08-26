package ru.fullrest.mfr.configurator.component

import javafx.fxml.FXMLLoader
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TreeView
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.configurator.component.tab.OptionTab
import ru.fullrest.mfr.configurator.component.tab.WorkTab
import ru.fullrest.mfr.configurator.extensions.correct
import ru.fullrest.mfr.configurator.extensions.listAllFiles
import ru.fullrest.mfr.configurator.extensions.walkAndBoldExisting
import ru.fullrest.mfr.javafx.component.FXMLComponent
import java.nio.file.Path

class FilePane(override val fxmlLoader: FXMLLoader) : FXMLComponent() {
    val fileTabPane: TabPane by fxml()

    val filesTreeTab: Tab by fxml()
    val usedFilesTreeTab: Tab by fxml()
    val freeFilesTreeTab: Tab by fxml()
    val excludeFilesTreeTab: Tab by fxml()

    val filesTree: TreeView<PathItem> by fxml()
    val usedFilesTree: TreeView<PathItem> by fxml()
    val freeFilesTree: TreeView<PathItem> by fxml()
    val excludeFilesList: ListView<Path> by fxml()

    init {
        filesTree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        usedFilesTree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        freeFilesTree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        excludeFilesList.selectionModel.selectionMode = SelectionMode.MULTIPLE
    }

    fun updateFilesTree(activeTab: WorkTab?, files: FileTree, filter: Set<Path>) {
        filesTree.correct(files, filter)
        val imageList = if (activeTab is OptionTab) {
            activeTab.contentItem.imagePath?.let { listOf(it) } ?: listOf()
        } else {
            listOf()
        }
        activeTab?.also { filesTree.root.walkAndBoldExisting(it.contentItem.files + imageList) }
    }

    fun updateUsedFilesTree(files: FileTree, filter: Set<Path>) {
        usedFilesTree.correct(files, filter)
    }

    fun updateFreeFilesTree(files: FileTree, exclude: Set<Path>) {
        freeFilesTree.correct(files, exclude)
    }

    fun updateExcludeFilesList(exclude: Set<Path>) {
        excludeFilesList.items.clear()
        excludeFilesList.items.addAll(exclude.sorted())
    }

    fun selectedItems(): Set<Path> {
        return fileTabPane.selectionModel.selectedItem?.let {
            when (it) {
                filesTreeTab -> filesTree
                usedFilesTreeTab -> usedFilesTree
                freeFilesTreeTab -> freeFilesTree
                else -> null
            }
        }?.selectionModel?.selectedItems?.flatMap { it.listAllFiles() }?.map { it.path }?.toSet() ?: emptySet()
    }

    companion object {
        private val log by Logger()
    }
}