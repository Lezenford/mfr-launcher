package com.lezenford.mfr.configurator.component

import com.lezenford.mfr.configurator.component.tab.FileListTab
import com.lezenford.mfr.configurator.component.tab.OptionTab
import com.lezenford.mfr.configurator.component.tab.WorkTab
import com.lezenford.mfr.configurator.content.AdditionalContent
import com.lezenford.mfr.configurator.content.Content
import com.lezenford.mfr.configurator.content.MainContent
import com.lezenford.mfr.configurator.content.SwitchableContent
import com.lezenford.mfr.configurator.controller.TextFieldController
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TreeItem
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority

class MainContentViewItem(content: MainContent) : ContentViewItem<MainContent>(content) {

    init {
        removeButton.isDisable = true
        removeButton.isVisible = false
        renameButton.isDisable = true
        renameButton.isVisible = false
    }
}

class AdditionalContentViewItem(
    content: AdditionalContent,
    buttonActions: (AdditionalContentViewItem) -> Pair<EventHandler<MouseEvent>, EventHandler<MouseEvent>>
) : ContentViewItem<AdditionalContent>(content) {

    init {
        val (renameButtonAction, removeButtonAction) = buttonActions(this)
        removeButton.onMouseClicked = removeButtonAction
        renameButton.onMouseClicked = renameButtonAction
    }
}

class SwitchableContentTreeItem(
    val switchableContent: SwitchableContent,
    private val buttonActions: (SwitchableContentTreeItem) -> Pair<EventHandler<MouseEvent>, EventHandler<MouseEvent>>
) : TreeItem<ContentViewItem<*>>() {
    val switchableContentTreeItem: ContentViewItem<*> = SwitchableContentViewItem(switchableContent)

    init {
        value = switchableContentTreeItem
        children.addAll(switchableContent.options.map { OptionalContentTreeItem(it) })
    }

    inner class SwitchableContentViewItem(content: SwitchableContent) :
        ContentViewItem<SwitchableContent>(content) {
        // override val tab: WorkTab by lazy { FileListTab(this) }

        private val addOptionButton: Button = Button("+").also {
            children.add(2, HBox().apply {
                children.add(it); alignment = Pos.CENTER
            })
        }

        init {
            addOptionButton.onMouseClicked = EventHandler {
                TextFieldController("Название опции") {
                    val option = SwitchableContent.Option(AdditionalContent(it))
                    if (switchableContent.options.add(option)) {
                        this@SwitchableContentTreeItem.children.add(OptionalContentTreeItem(option))
                    }
                }.showAndWait()
            }

            val (renameAction, removeAction) = buttonActions(this@SwitchableContentTreeItem)
            removeButton.onMouseClicked = removeAction
            renameButton.onMouseClicked = renameAction
        }
    }

    inner class OptionalContentTreeItem(option: SwitchableContent.Option) : TreeItem<ContentViewItem<*>>() {

        init {
            value = OptionContentViewItem(option)
        }

        inner class OptionContentViewItem(option: SwitchableContent.Option) :
            ContentViewItem<SwitchableContent.Option>(option) {

            init {
                removeButton.onMouseClicked = EventHandler {
                    switchableContent.options.remove(option)
                    this@SwitchableContentTreeItem.children.remove(this@OptionalContentTreeItem)
                }
                renameButton.onMouseClicked = EventHandler {
                    TextFieldController(content.name.value) { newName ->
                        if (content.name.value != newName) {
                            if (switchableContent.options.none { it.name.value == newName }) {
                                switchableContent.options.remove(content)
                                content.name.value = newName
                                switchableContent.options.add(content)
                            } else {
                                Alert(Alert.AlertType.ERROR, "Опция с названием $newName уже существует").showAndWait()
                            }
                        }
                    }.showAndWait()
                }
            }
        }
    }
}

abstract class ContentViewItem<T : Content>(val content: T) : HBox() {
    val removeButton: Button = Button("-")
    val renameButton: Button = Button("...")
    val label: Label = Label(content.name.value)

    init {
        content.name.addListener { _, _, newValue -> label.text = newValue }
        alignment = Pos.CENTER
        children.add(HBox().apply {
            children.add(label); setHgrow(this, Priority.ALWAYS); alignment = Pos.CENTER
        })
        children.add(HBox().apply {
            children.add(renameButton); alignment = Pos.CENTER
        })
        children.add(HBox().apply {
            children.add(removeButton); alignment = Pos.CENTER
        })
    }

    val name: String get() = content.name.value
}
