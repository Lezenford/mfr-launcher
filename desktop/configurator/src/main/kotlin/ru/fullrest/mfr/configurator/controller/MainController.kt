package ru.fullrest.mfr.configurator.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.TabPane
import javafx.scene.control.TreeItem
import javafx.scene.input.MouseButton
import javafx.stage.DirectoryChooser
import javafx.stage.StageStyle
import org.springframework.beans.factory.ObjectFactory
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.api.ContentType
import ru.fullrest.mfr.common.api.json.CONTENT_FILE_NAME
import ru.fullrest.mfr.common.api.json.Content
import ru.fullrest.mfr.common.api.json.SCHEMA_FILE_NAME
import ru.fullrest.mfr.common.api.json.Schema
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.md5
import ru.fullrest.mfr.common.extensions.toPath
import ru.fullrest.mfr.configurator.component.FilePane
import ru.fullrest.mfr.configurator.component.FileTree
import ru.fullrest.mfr.configurator.component.content.ContentListItem
import ru.fullrest.mfr.configurator.component.content.ContentPane
import ru.fullrest.mfr.configurator.component.content.ContentTreeItem
import ru.fullrest.mfr.configurator.component.content.OptionContentTreeItem
import ru.fullrest.mfr.configurator.component.content.PackageContentTreeItem
import ru.fullrest.mfr.configurator.component.tab.FileListTab
import ru.fullrest.mfr.configurator.component.tab.OptionTab
import ru.fullrest.mfr.configurator.component.tab.WorkTab
import ru.fullrest.mfr.configurator.extensions.listAllFiles
import ru.fullrest.mfr.javafx.component.FxController
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.io.path.writeLines
import kotlin.io.path.writeText

@Component
class MainController(private val progressControllerFactory: ObjectFactory<ProgressController>) :
    FxController(source = "fxml/main.fxml", stageStyle = StageStyle.DECORATED, css = null) {
    private val exclude: MutableSet<Path> = mutableSetOf(IGNORE_FILE_NAME.toPath(), CONTENT_FILE_NAME.toPath())

    private val filePane: FilePane = FilePane(fxmlLoader)
    private val contentPane: ContentPane = ContentPane(fxmlLoader)
    private val workTabPane: TabPane by fxml()
    private val gamePathLabel: Label by fxml()

    private val objectMapper = jacksonObjectMapper()

    private lateinit var gamePath: Path
    private lateinit var files: FileTree

    init {
        filePane.fileTabPane.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updateTrees()
        }
        contentPane.mainContentView.onMouseClicked = EventHandler { event ->
            if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                contentPane.mainContentView.selectionModel.selectedItem?.also { content ->
                    workTabPane.tabs.map { it as WorkTab }.find { it.contentItem == content }
                        ?.also { workTabPane.selectionModel.select(it) }
                        ?: FileListTab(content.name, content) { updateTrees() }.also {
                            workTabPane.tabs.add(it)
                            workTabPane.selectionModel.select(it)
                        }
                }
                updateTrees()
            }
        }
        contentPane.extraContentView.onMouseClicked = EventHandler { event ->
            if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                contentPane.extraContentView.selectionModel.selectedItem?.also { content ->
                    workTabPane.tabs.map { it as WorkTab }.find { it.contentItem == content }
                        ?.also { workTabPane.selectionModel.select(it) }
                        ?: FileListTab(content.name, content) { updateTrees() }.also {
                            workTabPane.tabs.add(it)
                            workTabPane.selectionModel.select(it)
                        }
                }
                updateTrees()
            }
        }
        contentPane.optionContentTree.onMouseClicked = EventHandler { event ->
            if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                contentPane.optionContentTree.selectionModel.selectedItem.value.takeIf { it is OptionContentTreeItem }
                    ?.let { it as OptionContentTreeItem }
                    ?.also { content ->
                        workTabPane.tabs.map { it as WorkTab }.find { it.contentItem == content }
                            ?.also { workTabPane.selectionModel.select(it) }
                            ?: OptionTab(
                                name = content.name,
                                contentItem = content,
                                gameDirectory = gamePath,
                                selectedTreeItems = { filePane.selectedItems() },
                                updateTree = { updateTrees() }
                            ).also {
                                workTabPane.tabs.add(it)
                                workTabPane.selectionModel.select(it)
                            }
                    }
                updateTrees()
            }
        }
        workTabPane.selectionModel.selectedItemProperty().addListener { _, _, _ ->
            updateTrees()
        }
    }

    fun selectGame() {
        DirectoryChooser().apply {
            initialDirectory = File("").absoluteFile
            title = "Выберите папку игры"
        }.showDialog(stage)?.also { file ->
            gamePath = file.toPath()
            gamePath.resolve(IGNORE_FILE_NAME).takeIf { it.exists() }?.readLines()?.map { it.toPath() }
                ?.also { exclude.addAll(it) }
            createFileTree(file.toPath())
            gamePathLabel.text = file.absolutePath
            val allFiles = filePane.filesTree.root.listAllFiles().map { it.path }.toSet()
            gamePath.resolve(CONTENT_FILE_NAME).takeIf { it.exists() }?.readText()
                ?.let { objectMapper.readValue<Content>(it) }
                ?.also { content ->
                    content.categories.forEach { category ->
                        when (category.type) {
                            ContentType.MAIN -> contentPane.mainContentView
                            ContentType.EXTRA -> contentPane.extraContentView
                            else -> null
                        }?.also { view ->
                            category.items.forEach { categoryItem ->
                                val item: ContentListItem = view.items.find { it.name == categoryItem.name }
                                    ?: ContentListItem(categoryItem.name, view).also { view.items.add(it) }
                                item.apply {
                                    files.addAll(categoryItem.files.map { it.path.toPath() }
                                        .filter { allFiles.contains(it) })
                                }
                            }
                        }
                    }
                }
            gamePath.resolve(SCHEMA_FILE_NAME).takeIf { it.exists() }?.readText()
                ?.let { objectMapper.readValue<Schema>(it) }
                ?.also { schema ->
                    schema.packages.forEach { `package` ->
                        val packageTreeItem =
                            contentPane.optionContentTree.root.children.find { it.value.name == `package`.name }
                                ?: TreeItem<ContentTreeItem>(
                                    PackageContentTreeItem(
                                        `package`.name,
                                        contentPane.optionContentTree.root,
                                        contentPane.mainContentView.items.first().files
                                    )
                                ).also { contentPane.optionContentTree.root.children.add(it) }
                        `package`.options.forEach { option ->
                            val optionItem: OptionContentTreeItem = packageTreeItem.children.map { it.value }
                                .filterIsInstance<OptionContentTreeItem>()
                                .find { it.name == option.name }
                                ?: OptionContentTreeItem(
                                    option.name,
                                    packageTreeItem,
                                    contentPane.mainContentView.items.first().files
                                ).also { packageTreeItem.children.add(TreeItem(it)) }
                            val files = option.items.filter { allFiles.contains(it.storagePath.toPath()) }
                            optionItem.addFiles(files.map { it.storagePath.toPath() })
                            optionItem.description = option.description
                            optionItem.imagePath = option.image?.toPath()
                            files.forEach { file ->
                                optionItem.addFileDetails(file.storagePath.toPath(), file.gamePath, file.storagePath)
                            }
                        }
                    }
                }
        }
    }

    fun addSelectedFiles() {
        workTabPane.selectionModel.selectedItem?.let { it as WorkTab }?.addFiles(filePane.selectedItems())
        updateTrees()
    }

    fun removeSelectedFiles() {
        workTabPane.selectionModel.selectedItem?.let { it as WorkTab }?.removeFiles(filePane.selectedItems())
        updateTrees()
    }

    fun addFilesToExclude() {
        filePane.apply {
            when (val selectedItem = fileTabPane.selectionModel.selectedItem) {
                filesTreeTab, usedFilesTreeTab, freeFilesTreeTab -> {
                    when (selectedItem) {
                        filesTreeTab -> filesTree
                        usedFilesTreeTab -> usedFilesTree
                        freeFilesTreeTab -> freeFilesTree
                        else -> throw IllegalArgumentException("Unknown tab ${selectedItem.text}")
                    }.selectionModel.selectedItems.map { it.value.path }.also {
                        exclude.addAll(it)
                        contentPane.removeFiles(it)
                    }
                }
                excludeFilesTreeTab -> {
                    excludeFilesList.selectionModel.selectedItems.also { exclude.removeAll(it) }
                }
            }
            updateTrees()
        }
    }

    fun createExtraCategory() {
        TextFieldController { contentPane.addExtraContentItem(it) }.showAndWait()
    }

    fun createOptionPackage() {
        TextFieldController { contentPane.addOptionalContentPackage(it) }.showAndWait()
    }

    fun updateTrees() {
        filePane.apply {
            when (fileTabPane.selectionModel.selectedItem) {
                filesTreeTab -> updateFilesTree(workTabPane.selectionModel.selectedItem.takeIf { it is WorkTab }
                    ?.let { it as WorkTab }, files, files.invertFilter(exclude))
                usedFilesTreeTab -> updateUsedFilesTree(files, contentPane.allFiles())
                freeFilesTreeTab -> updateFreeFilesTree(files, files.invertFilter(contentPane.allFiles() + exclude))
                excludeFilesTreeTab -> updateExcludeFilesList(exclude)
            }
        }
    }

    fun save() {
        progressControllerFactory.`object`.execute {
            saveIgnore()
            saveSchema()
            saveStructure()
        }
    }

    private fun saveIgnore() {
        gamePath.resolve(IGNORE_FILE_NAME).writeLines(exclude.map { it.toString() })
    }

    private fun saveSchema() {
        Schema(
            packages = contentPane.optionContentTree.root.children.map { `package` ->
                Schema.Package(
                    name = `package`.value.name,
                    options = `package`.children.map { it.value }
                        .filterIsInstance<OptionContentTreeItem>()
                        .map { option ->
                            Schema.Package.Option(
                                name = option.name,
                                description = option.description,
                                image = option.imagePath.toString(),
                                items = option.fileMap.map {
                                    Schema.Package.Option.Item(
                                        storagePath = it.value.second,
                                        gamePath = it.value.first,
                                        md5 = gamePath.resolve(it.key).md5()
                                    )
                                }
                            )
                        }
                )
            },
            extra = contentPane.extraContentView.items.map {
                Schema.Extra(
                    name = it.name,
                    items = it.files.map { file ->
                        Schema.Extra.Item(
                            path = file.toString(),
                            md5 = gamePath.resolve(file).md5()
                        )
                    }
                )
            }
        ).also {
            gamePath.resolve(SCHEMA_FILE_NAME).writeText(objectMapper.writeValueAsString(it))
        }
    }

    private fun saveStructure() {
        contentPane.mainContentView.items.first().files.add(SCHEMA_FILE_NAME.toPath())
        val content = Content(
            categories = listOf(
                contentPane.mainContentView.items.let { categories ->
                    Content.Category(
                        type = ContentType.MAIN,
                        required = true,
                        items = categories.map { category ->
                            Content.Category.Item(
                                name = category.name,
                                files = category.files.map {
                                    Content.Category.Item.File(
                                        path = it.toString()
                                    )
                                })
                        })
                },
                contentPane.extraContentView.items.let { categories ->
                    Content.Category(
                        type = ContentType.EXTRA,
                        required = false,
                        items = categories.map { category ->
                            Content.Category.Item(
                                name = category.name,
                                files = category.files.map {
                                    Content.Category.Item.File(
                                        path = it.toString()
                                    )
                                })
                        })
                },
                contentPane.optionContentTree.root.children.map { it.value }.let { categories ->
                    Content.Category(
                        type = ContentType.OPTIONAL,
                        required = false,
                        categories.map { category ->
                            Content.Category.Item(
                                name = category.name,
                                files = category.files.map {
                                    Content.Category.Item.File(
                                        path = it.toString()
                                    )
                                })
                        })
                },
            )
        )
        gamePath.resolve(CONTENT_FILE_NAME).writeBytes(objectMapper.writeValueAsBytes(content))
    }

    private fun createFileTree(root: Path) {
        FileTree(pathToFileTree(root)).also { files = it }
        files.toTreeItem(files.invertFilter(exclude)).also { filePane.filesTree.root = it }
        files.toTreeItem(contentPane.allFiles()).also { filePane.usedFilesTree.root = it }
        files.toTreeItem(files.invertFilter(contentPane.allFiles())).also { filePane.freeFilesTree.root = it }
        filePane.updateExcludeFilesList(exclude)
    }

    private fun pathToFileTree(path: Path): FileTree.Node {
        var result: FileTree.Node = FileTree.Node(path)
        Files.walkFileTree(path, object : FileVisitor<Path> {
            private val stack = LinkedList<MutableList<FileTree.Node>>()
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes?): FileVisitResult =
                FileVisitResult.CONTINUE.apply {
                    stack.addLast(mutableListOf())
                }

            override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult =
                FileVisitResult.CONTINUE.apply {
                    stack.last.add(FileTree.Node(gamePath.relativize(file)))
                }

            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                throw IllegalArgumentException("Visit to file $file was failed", exc)
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult =
                FileVisitResult.CONTINUE.apply {
                    stack.removeLast()?.takeIf { it.isNotEmpty() }
                        ?.let { FileTree.Node(gamePath.relativize(dir), it) }
                        ?.also {
                            if (stack.isEmpty()) {
                                result = it
                            } else {
                                if (it.children.isNotEmpty()) {
                                    stack.last.add(it)
                                }
                            }
                        }
                }
        })
        return result
    }

    companion object {
        private val log by Logger()
        private const val IGNORE_FILE_NAME = ".configuratorignore"
    }
}