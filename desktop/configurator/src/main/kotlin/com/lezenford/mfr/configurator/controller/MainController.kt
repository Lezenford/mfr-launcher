package com.lezenford.mfr.configurator.controller

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.configurator.component.SwitchableContentTreeItem
import com.lezenford.mfr.configurator.component.pane.ContentPane
import com.lezenford.mfr.configurator.component.pane.FilePane
import com.lezenford.mfr.configurator.component.tab.FileListTab
import com.lezenford.mfr.configurator.component.tab.OptionTab
import com.lezenford.mfr.configurator.component.tab.WorkTab
import com.lezenford.mfr.configurator.content.AdditionalContent
import com.lezenford.mfr.configurator.content.MainContent
import com.lezenford.mfr.configurator.content.SwitchableContent
import com.lezenford.mfr.configurator.service.ContentService
import com.lezenford.mfr.configurator.service.FileTreeService
import com.lezenford.mfr.configurator.service.IgnoreService
import com.lezenford.mfr.javafx.annotation.UIComponent
import com.lezenford.mfr.javafx.component.FxController
import javafx.event.EventHandler
import javafx.scene.control.Label
import javafx.scene.control.TabPane
import javafx.scene.input.MouseButton
import javafx.stage.StageStyle
import org.springframework.beans.factory.ObjectFactory

@UIComponent
class MainController(
    private val progressControllerFactory: ObjectFactory<ProgressController>,
    private val fileTreeService: FileTreeService,
    private val ignoreService: IgnoreService,
    private val contentService: ContentService,
    mainContent: MainContent,
    additionalContents: MutableSet<AdditionalContent>,
    switchableContents: MutableSet<SwitchableContent>
) : FxController(source = "fxml/main.fxml", stageStyle = StageStyle.DECORATED, css = null) {
    private val workTabPane: TabPane by fxml()
    private val filePane: FilePane =
        FilePane(mainContent, additionalContents, switchableContents, fileTreeService, ignoreService, fxmlLoader) {
            workTabPane.selectionModel.selectedItem.takeIf { it is WorkTab }
                ?.let { it as WorkTab }?.contentViewItem?.content
        }
    private val contentPane: ContentPane = ContentPane(mainContent, additionalContents, switchableContents, fxmlLoader)
    private val gamePathLabel: Label by fxml()

    init {
        contentPane.mainContentView.onMouseClicked = EventHandler { event ->
            if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                contentPane.mainContentView.selectionModel.selectedItem.also { selectedItem ->
                    val tab = workTabPane.tabs.find { it is WorkTab && it.contentViewItem === selectedItem }
                        ?: FileListTab(selectedItem).also { workTabPane.tabs.add(it) }
                            .also { it.contentChangeObserver.addListener { filePane.updateActiveFilesTree() } }
                    workTabPane.selectionModel.select(tab)
                }
            }
        }
        contentPane.additionalContentView.onMouseClicked = EventHandler { event ->
            if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                contentPane.additionalContentView.selectionModel.selectedItem.also { selectedItem ->
                    val tab = workTabPane.tabs.find { it is WorkTab && it.contentViewItem === selectedItem }
                        ?: FileListTab(selectedItem).also { workTabPane.tabs.add(it) }
                            .also { it.contentChangeObserver.addListener { filePane.updateActiveFilesTree() } }
                    workTabPane.selectionModel.select(tab)
                }
            }
        }
        contentPane.switchableContentTree.onMouseClicked = EventHandler { event ->
            if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                val viewItem = contentPane.switchableContentTree.selectionModel.selectedItem.value
                if (viewItem is SwitchableContentTreeItem.OptionalContentTreeItem.OptionContentViewItem) {
                    viewItem.also { selectedItem ->
                        val tab = workTabPane.tabs.find { it is WorkTab && it.contentViewItem === selectedItem }
                            ?: OptionTab(selectedItem) { filePane.getSelectedFiles() }.also { workTabPane.tabs.add(it) }
                                .also { it.contentChangeObserver.addListener { filePane.updateActiveFilesTree() } }
                        workTabPane.selectionModel.select(tab)
                    }
                }
            }
        }
        workTabPane.selectionModel.selectedItemProperty().addListener { _,_,_ -> filePane.updateActiveFilesTree() }
    }

    override fun onShowing() {
        gamePathLabel.text = fileTreeService.tree.root.absolutePath.toString()
        filePane.updateActiveFilesTree()
        contentPane.updateContentViews()
    }

    fun addSelectedFiles() {
        workTabPane.selectionModel.selectedItem?.let { it as WorkTab }?.addFiles(filePane.getSelectedFiles())
    }

    fun removeSelectedFiles() {
        workTabPane.selectionModel.selectedItem?.let { it as WorkTab }?.removeFiles(filePane.getSelectedFiles())
    }

    fun addFilesToExclude() {
        filePane.addFilesToExclude()
        filePane.updateActiveFilesTree()
    }

    fun createAdditionalContent() = contentPane.addAdditionalContent()

    fun createSwitchableContent() = contentPane.addSwitchableContent()

    fun save() {
        progressControllerFactory.`object`.execute {
            if (contentService.validate()) {
                ignoreService.saveIgnoreFile()
                contentService.saveSchema()
                contentService.saveContent()
            }
        }
    }

    companion object {
        private val log by Logger()
    }
}