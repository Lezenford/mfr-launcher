package com.lezenford.mfr.configurator.component.tab

import com.lezenford.mfr.common.extensions.toPath
import com.lezenford.mfr.configurator.component.SwitchableContentTreeItem
import com.lezenford.mfr.configurator.content.GameFile
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
import org.apache.commons.io.FileUtils
import java.io.File
import kotlin.math.min

class OptionTab(
    override val contentViewItem: SwitchableContentTreeItem.OptionalContentTreeItem.OptionContentViewItem,
    private val selectedTreeItems: () -> Set<GameFile>
) : WorkTab(contentViewItem.name) {
    private val description: TextArea = TextArea()
    private val imageWrapper: VBox = VBox()
    private val commonCheckBox: CheckBox = CheckBox()
    private val gamePrefix: TextField = TextField()
    private val storagePrefix: TextField = TextField()
    private val listView: ListView<ListViewRow> = ListView()
    private val fileItems: MutableMap<GameFile, ListViewRow> = hashMapOf()
    private val selectedItems: MutableSet<ListViewRow> = hashSetOf()

    init {
        contentViewItem.content.name.addListener { _, _, newValue -> text = newValue }

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
                                            .mapTo(hashSetOf()) { it.gameFile }
                                            .also { removeFiles(it) }
                                        commonCheckBox.isSelected = false
                                        contentChangeObserver.update()
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
            contentViewItem.content.description = newValue
        }
        description.text = contentViewItem.content.description
        contentViewItem.content.image?.also {
            setImage(it)
        }
        contentViewItem.content.files.firstOrNull()?.also {
            val first = it.relativePath.toString().split(File.separator).reversed()
            val second =
               contentViewItem.content.alternativePaths[it.id]?.toString()?.split(File.separator)?.reversed() ?: emptyList()
            val same = mutableListOf<String>()
            for (i in 0 until min(first.size, second.size)) {
                if (first[i] == second[i]) {
                    same.add(first[i])
                } else {
                    break
                }
            }
            val sameString = same.reversed().joinToString(File.separator)
            gamePrefix.text = contentViewItem.content.alternativePaths[it.id]?.toString()?.replace(sameString, "") ?: ""
            storagePrefix.text = it.relativePath.toString().replace(sameString, "")
        }
        addFiles(contentViewItem.content.files)
    }

    private fun setImage(file: GameFile) {
        imageWrapper.children.clear()
        imageWrapper.children.add(ImageView(Image(file.absolutePath.toUri().toURL().toString())))
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
        val gamePath: TextField = TextField().also {
            setHgrow(it, Priority.ALWAYS)
            it.isEditable = false
        }
        val storagePath: TextField = TextField().also {
            it.text = gameFile.relativePath.toString()
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
                    gamePrefix.text.toPath().resolve(storagePrefix.text.toPath().relativize(gameFile.relativePath))
                }.getOrNull()?.toString() ?: gameFile.toString()
            contentViewItem.content.alternativePaths[gameFile.id] = gamePath.text.toPath()
        }
    }
}