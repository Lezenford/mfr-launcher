package com.lezenford.mfr.configurator.component.pane

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.configurator.content.AdditionalContent
import com.lezenford.mfr.configurator.content.Content
import com.lezenford.mfr.configurator.content.GameFile
import com.lezenford.mfr.configurator.content.GameFolder
import com.lezenford.mfr.configurator.content.MainContent
import com.lezenford.mfr.configurator.content.SwitchableContent
import com.lezenford.mfr.configurator.service.FileTreeService
import com.lezenford.mfr.configurator.service.IgnoreService
import com.lezenford.mfr.javafx.component.FXMLComponent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.SelectionMode
import javafx.scene.control.Separator
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import java.nio.file.Path
import java.util.LinkedList

class FilePane(
    private val mainContent: MainContent,
    private val additionalContents: MutableSet<AdditionalContent>,
    private val switchableContents: MutableSet<SwitchableContent>,
    private val fileTreeService: FileTreeService,
    private val ignoreService: IgnoreService,
    override val fxmlLoader: FXMLLoader,
    private val activeContent: () -> Content?
) : FXMLComponent() {
    val fileTabPane: TabPane by fxml()

    val filesTreeTab: Tab by fxml()
    val usedFilesTreeTab: Tab by fxml()
    val freeFilesTreeTab: Tab by fxml()
    val excludeFilesTreeTab: Tab by fxml()

    val filesTree: TreeView<FileTreeItem> by fxml()
    val usedFilesTree: TreeView<FileTreeItem> by fxml()
    val freeFilesTree: TreeView<FileTreeItem> by fxml()
    val excludeFilesList: ListView<ExcludeFileViewItem> by fxml()

    init {
        filesTree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        usedFilesTree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        freeFilesTree.selectionModel.selectionMode = SelectionMode.MULTIPLE
        excludeFilesList.selectionModel.selectionMode = SelectionMode.MULTIPLE

        excludeFilesTreeTab.selectedProperty().addListener { _, _, newValue -> if (newValue) updateExcludeFiles() }
        fileTabPane.selectionModel.selectedItemProperty().addListener { _, _, _ -> updateActiveFilesTree() }
    }

    fun updateActiveFilesTree() {
        val usedFiles = usedFiles

        findActiveTree()?.also { treeView ->
            val tree = fileTreeService.tree
            when (treeView) {
                usedFilesTree -> {
                    tree.forEach { (key, value) -> if (key !in usedFiles && value is GameFile) value.ignore = true }
                }

                freeFilesTree -> {
                    usedFiles.forEach { tree[it]?.ignore = true }
                }
            }

            val openedNodes = hashSetOf<Int>()
            val stackTreeItems: LinkedList<TreeItem<FileTreeItem>> = LinkedList()
            if (treeView.root != null) {
                stackTreeItems.add(treeView.root)
                while (stackTreeItems.isNotEmpty()) {
                    val treeItem = stackTreeItems.removeLast()
                    treeItem.children.forEach { stackTreeItems.addLast(it) }
                    if (treeItem.isExpanded && treeItem.value != null) openedNodes.add(treeItem.value.id)
                }
            }

            val stackFolder = LinkedList<GameFolder>()
            stackFolder.add(tree.root)
            stackTreeItems.add(TreeItem<FileTreeItem>().also { treeView.root = it })
            while (stackFolder.isNotEmpty()) {
                val gameFolder = stackFolder.removeLast()
                val treeItem = stackTreeItems.removeLast()
                if (treeItem.value != null && treeItem.value.id in openedNodes) treeItem.isExpanded = true
                gameFolder.child.forEach {
                    if (!it.hidden) {
                        val item = TreeItem(FileTreeItem(it.id, it.name))
                        treeItem.children.add(item)
                        if (it is GameFolder) {
                            stackFolder.addLast(it)
                            stackTreeItems.addLast(item)
                        }
                    }
                }
            }

            if (treeView == filesTree) {
                activeContent()?.files?.onEach { tree[it.id]?.ignore = true }?.also {
                    stackTreeItems.add(treeView.root)
                    while (stackTreeItems.isNotEmpty()) {
                        val treeItem = stackTreeItems.removeLast()
                        treeItem.children.forEach { stackTreeItems.addLast(it) }
                        if (treeItem.value != null) {
                            if (tree[treeItem.value.id]?.hidden == true) {
                                treeItem.value.style = "-fx-font-weight: bold"
                            } else {
                                treeItem.value.style = ""
                            }
                        }
                    }
                }
            }
        }
    }

    fun addFilesToExclude() {
        val tree = fileTreeService.tree
        findActiveTree()?.selectionModel?.selectedItems?.mapNotNull { tree[it.value.id]?.relativePath }?.also {
            ignoreService.addToIgnore(it)
        }
        updateExcludeFiles()
    }

    fun getSelectedFiles(): Set<GameFile> {
        val activeElements =
            findActiveTree()?.selectionModel?.selectedItems?.mapTo(mutableSetOf()) { it.value.id } ?: emptySet()
        val tree = fileTreeService.tree
        val files = activeElements.mapTo(mutableSetOf()) { tree[it] }
        val gameFiles = mutableSetOf<GameFile>()
        while (files.isNotEmpty()) {
            when (val file = files.first()!!.also { files.remove(it) }) {
                is GameFile -> gameFiles.add(file)
                is GameFolder -> files.addAll(file.child)
            }
        }
        return gameFiles
    }

    private val usedFiles: Set<Int>
        get() {
            val usedFiles = mutableSetOf<Int>()
            mainContent.files.mapTo(usedFiles) { it.id }
            additionalContents.forEach { content -> content.files.mapTo(usedFiles) { it.id } }
            switchableContents.forEach { content ->
                content.files.mapTo(usedFiles) { it.id }
                content.options.mapNotNullTo(usedFiles) { it.image?.id }
            }
            return usedFiles
        }

    private fun updateExcludeFiles() {
        excludeFilesList.items.clear()
        ignoreService.ignoreFiles.map {
            excludeFilesList.items.add(ExcludeFileViewItem(it))
        }
    }

    private fun findActiveTree(): TreeView<FileTreeItem>? {
        return when (fileTabPane.selectionModel.selectedItem) {
            filesTreeTab -> filesTree
            usedFilesTreeTab -> usedFilesTree
            freeFilesTreeTab -> freeFilesTree
            else -> null
        }
    }

    class FileTreeItem(val id: Int, name: String) : Label(name)

    inner class ExcludeFileViewItem(private val path: Path) : HBox() {
        private val removeButton: Button = Button("-")

        init {
            children.add(
                HBox().also { it.children.add(removeButton) }
            )
            children.add(
                Separator(Orientation.VERTICAL)
            )
            children.add(
                HBox().also { it.children.add(Label(path.toString())); setHgrow(it, Priority.ALWAYS) }
            )

            removeButton.onMouseClicked = EventHandler {
                ignoreService.removeFromIgnore(listOf(path))
                excludeFilesList.items.remove(this)
                updateExcludeFiles()
            }
        }
    }

    companion object {
        private val log by Logger()
    }
}