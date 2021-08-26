package ru.fullrest.mfr.configurator.component.tab

import javafx.event.EventHandler
import javafx.geometry.Orientation
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane
import javafx.scene.control.Separator
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.ToolBar
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import ru.fullrest.mfr.common.extensions.toPath
import ru.fullrest.mfr.configurator.component.content.OptionContentTreeItem
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.math.min

class OptionTab(
    name: String,
    override val contentItem: OptionContentTreeItem,
    private val gameDirectory: Path,
    private val selectedTreeItems: () -> Set<Path>,
    updateTree: () -> Unit
) : WorkTab(name, contentItem) {
    private val description: TextArea = TextArea()
    private var imagePath: Path? = null
        set(value) {
            field = value.also { contentItem.imagePath = value }
        }

    private val imageWrapper: VBox = VBox()
    private val commonCheckBox: CheckBox = CheckBox()
    private val gamePrefix: TextField = TextField()
    private val storagePrefix: TextField = TextField()
    private val listView: ListView<ListViewRow> = ListView()
    private val fileItems: MutableMap<Path, ListViewRow> = mutableMapOf()
    private val selectedItems: MutableSet<ListViewRow> = mutableSetOf()

    init {
        content = ScrollPane().apply {
            isFitToHeight = true
            isFitToWidth = true
            content = VBox().apply {
                VBox.setVgrow(this, Priority.ALWAYS)
                children.addAll(
                    HBox().also { box ->
                        box.minWidth = 437.0 * 2.0
                        box.children.addAll(
                            VBox(
                                HBox(
                                    Button("-").also { button ->
                                        button.onMouseClicked = EventHandler {
                                            description.text = ""
                                        }
                                    },
                                    Separator(Orientation.VERTICAL),
                                    Label("Описание")
                                ),
                                description.also {
                                    it.minHeight = 288.0
                                    it.maxHeight = 288.0
                                    it.minWidth = 437.0
                                }).also {
                                HBox.setHgrow(it, Priority.ALWAYS)
                            },
                            Separator(Orientation.VERTICAL),
                            VBox(
                                HBox(
                                    Button("+").also { button ->
                                        button.onMouseClicked = EventHandler {
                                            selectedTreeItems().singleOrNull()?.takeIf { it.name.endsWith(".png") }
                                                ?.also {
                                                    setImage(it)
                                                } ?: Alert(
                                                Alert.AlertType.INFORMATION,
                                                "Для добавления изображения выберите png файл в дереве файлов слева и снова нажмите на кнопку"
                                            ).showAndWait()
                                        }
                                    },
                                    Separator(Orientation.VERTICAL),
                                    Label("Изображение")
                                ),
                                imageWrapper.also {
                                    it.minHeight = 288.0
                                    it.maxHeight = 288.0
                                    it.minWidth = 437.0
                                    it.maxWidth = 437.0
                                    it.style = "-fx-background-color: white"
                                }).also {
                                it.minWidth = 437.0
                                it.maxWidth = 437.0
                                HBox.setHgrow(it, Priority.NEVER)
                            }
                        )
                    },
                    VBox().also { filesBox ->
                        VBox.setVgrow(filesBox, Priority.ALWAYS)
                        filesBox.children.addAll(
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
                                        commonCheckBox.isSelected = false
                                        updateTree()
                                    }
                                },
                            ),
                            HBox().apply {
                                children.addAll(
                                    CheckBox().also { it.isVisible = false },
                                    Separator(Orientation.VERTICAL),
                                    HBox(Label("Префикс в папке с игрой")).also { HBox.setHgrow(it, Priority.ALWAYS) },
                                    HBox(Label("Префикс в папке Optional")).also { HBox.setHgrow(it, Priority.ALWAYS) }
                                )
                            },
                            HBox().apply {
                                children.addAll(
                                    CheckBox().also { it.isVisible = false },
                                    Separator(Orientation.VERTICAL),
                                    gamePrefix.also { HBox.setHgrow(it, Priority.ALWAYS) },
                                    storagePrefix.also { HBox.setHgrow(it, Priority.ALWAYS) }
                                )
                            },
                            HBox().apply {
                                children.addAll(
                                    CheckBox().also { it.isVisible = false },
                                    Separator(Orientation.VERTICAL),
                                    HBox(Label("Путь в папке с игрой")).also { HBox.setHgrow(it, Priority.ALWAYS) },
                                    HBox(Label("Путь в папке Optional")).also { HBox.setHgrow(it, Priority.ALWAYS) }
                                )
                            },
                            listView.apply {
                                VBox.setVgrow(this, Priority.ALWAYS)
                            }
                        )
                    }
                )
            }
        }
        gamePrefix.textProperty().addListener { _, _, _ ->
            listView.items.forEach { it.updatePath() }
        }
        storagePrefix.textProperty().addListener { _, _, _ ->
            listView.items.forEach { it.updatePath() }
        }
        gamePrefix.text = "Data Files"
        description.textProperty().addListener { _, _, newValue ->
            contentItem.description = newValue
        }
        description.text = contentItem.description
        contentItem.imagePath?.also {
            setImage(it)
        }
        contentItem.fileMap.values.firstOrNull()?.also {
            val first = it.first.toPath().toString().split(File.separator).reversed()
            val second = it.second.toPath().toString().split(File.separator).reversed()
            val same = mutableListOf<String>()
            for (i in 0 until min(first.size, second.size)) {
                if (first[i] == second[i]) {
                    same.add(first[i])
                } else {
                    break
                }
            }
            val sameString = same.reversed().joinToString(File.separator)
            gamePrefix.text = it.first.replace(sameString, "")
            storagePrefix.text = it.second.replace(sameString, "")
        }
        addFiles(contentItem.files)
    }

    private fun setImage(path: Path) {
        imagePath = path
        imageWrapper.children.clear()
        imageWrapper.children.add(
            ImageView(
                Image(
                    gameDirectory.resolve(path).toUri().toURL().toString()
                )
            )
        )
    }

    override fun addFiles(files: Collection<Path>) {
        contentItem.addFiles(files)
        (files - fileItems.keys).forEach {
            ListViewRow(it).also { row ->
                listView.items.add(row)
                fileItems[it] = row
            }
        }
    }

    override fun removeFiles(files: Collection<Path>) {
        contentItem.removeFiles(files)
        files.forEach {
            fileItems.remove(it)?.also { row ->
                listView.items.remove(row)
                selectedItems.remove(row)
            }
        }
    }

    private inner class ListViewRow(val path: Path) : HBox() {
        val checkBox = CheckBox()
        val gamePath: TextField = TextField().also {
            setHgrow(it, Priority.ALWAYS)
            it.isEditable = false
        }
        val storagePath: TextField = TextField().also {
            it.text = path.toString()
            it.isEditable = false
            setHgrow(it, Priority.ALWAYS)
        }

        init {
            setHgrow(this, Priority.ALWAYS)
            children.addAll(
                checkBox.also { checkBox ->
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
                HBox().also {
                    setHgrow(it, Priority.ALWAYS)
                    it.children.add(gamePath)
                },
                HBox().also {
                    setHgrow(it, Priority.ALWAYS)
                    it.children.addAll(storagePath)
                }
            )
            updatePath()
        }

        fun updatePath() {
            gamePath.text =
                kotlin.runCatching {
                    gamePrefix.text.toPath().resolve(storagePrefix.text.toPath().relativize(path))
                }.getOrNull()?.toString() ?: path.toString()
            contentItem.addFileDetails(path, gamePath.text, storagePath.text)
        }
    }
}