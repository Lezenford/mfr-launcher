package com.lezenford.mfr.configurator.component.pane

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.configurator.component.AdditionalContentViewItem
import com.lezenford.mfr.configurator.component.ContentViewItem
import com.lezenford.mfr.configurator.component.MainContentViewItem
import com.lezenford.mfr.configurator.component.SwitchableContentTreeItem
import com.lezenford.mfr.configurator.content.AdditionalContent
import com.lezenford.mfr.configurator.content.Content
import com.lezenford.mfr.configurator.content.MainContent
import com.lezenford.mfr.configurator.content.SwitchableContent
import com.lezenford.mfr.configurator.controller.TextFieldController
import com.lezenford.mfr.javafx.component.FXMLComponent
import javafx.beans.binding.Bindings
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.control.Alert
import javafx.scene.control.ListView
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox

class ContentPane(
    private val mainContent: MainContent,
    private val additionalContents: MutableSet<AdditionalContent>,
    private val switchableContents: MutableSet<SwitchableContent>,
    override val fxmlLoader: FXMLLoader
) : FXMLComponent() {
    val mainContentView: ListView<MainContentViewItem> by fxml()
    val additionalContentView: ListView<AdditionalContentViewItem> by fxml()
    val switchableContentTree: TreeView<ContentViewItem<*>> by fxml()

    init {
        mainContentView.apply {
            prefHeight = 35.0
            minHeight = 35.0
            maxHeight = 35.0
            fixedCellSize = 35.0
        }
        mainContentView.items.add(MainContentViewItem(mainContent))
        additionalContentView.apply {
            prefHeight = 70.0
            minHeight = 35.0
            maxHeight = 140.0
            fixedCellSize = 35.0
        }
        additionalContents.forEach { additionalContentView.items.add(createAdditionalContentViewItem(it)) }
        switchableContentTree.root = TreeItem()
        switchableContents.forEach { switchableContentTree.root.children.add(createSwitchableContentTreeItem(it)) }
    }

    fun updateContentViews() {
        additionalContentView.items.clear()
        additionalContents.forEach {
            additionalContentView.items.add(createAdditionalContentViewItem(it))
        }

        switchableContentTree.root.children.clear()
        switchableContents.forEach {
            switchableContentTree.root.children.add(createSwitchableContentTreeItem(it))
        }
    }

    fun addAdditionalContent() {
        TextFieldController("Название контента") {
            val additionalContent = AdditionalContent(it)
            if (additionalContents.add(additionalContent)) {
                additionalContentView.items.add(createAdditionalContentViewItem(additionalContent))
            }
        }.showAndWait()
    }

    fun addSwitchableContent() {
        TextFieldController("Название пакета") {
            val switchableContent = SwitchableContent(it)
            if (switchableContents.add(switchableContent)) {
                val switchableContentTreeItem = createSwitchableContentTreeItem(switchableContent)
                switchableContentTree.root.children.add(switchableContentTreeItem)
                switchableContent.options.forEach { option ->
                    switchableContentTreeItem.children.add(
                        switchableContentTreeItem.OptionalContentTreeItem(option)
                    )
                }
            }
        }.showAndWait()
    }

    private fun createAdditionalContentViewItem(content: AdditionalContent): AdditionalContentViewItem {
        return AdditionalContentViewItem(content) { viewItem ->
            content.renameEvent(additionalContents) to EventHandler<MouseEvent> {
                additionalContentView.items.remove(viewItem)
                additionalContents.remove(content)
            }
        }
    }

    private fun createSwitchableContentTreeItem(content: SwitchableContent): SwitchableContentTreeItem {
        return SwitchableContentTreeItem(content) { treeItem ->
            content.renameEvent(switchableContents) to EventHandler<MouseEvent> {
                switchableContentTree.root.children.remove(treeItem)
                switchableContents.remove(treeItem.switchableContent)
            }
        }
    }

    private fun <T : Content> T.renameEvent(collection: MutableSet<T>): EventHandler<MouseEvent> {
        return EventHandler<MouseEvent> {
            TextFieldController(name.value) { newName ->
                if (name.value != newName) {
                    if (collection.none { it.name.value == newName }) {
                        collection.remove(this)
                        name.value = newName
                        collection.add(this)
                    } else {
                        Alert(Alert.AlertType.ERROR, "Опция с названием $newName уже существует").showAndWait()
                    }
                }
            }.showAndWait()
        }
    }

    companion object {
        private val log by Logger()
    }
}