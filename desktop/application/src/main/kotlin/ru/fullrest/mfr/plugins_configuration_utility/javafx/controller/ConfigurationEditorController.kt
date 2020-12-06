package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.beans.value.ObservableValue
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.util.Duration
import javafx.util.StringConverter
import org.springframework.beans.factory.annotation.Autowired
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Details
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Group
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.DetailsRepository
import ru.fullrest.mfr.plugins_configuration_utility.model.repository.ReleaseRepository
import ru.fullrest.mfr.plugins_configuration_utility.service.FileService
import ru.fullrest.mfr.plugins_configuration_utility.service.GroupService
import ru.fullrest.mfr.plugins_configuration_utility.util.listAllFiles
import java.io.File
import java.net.MalformedURLException
import java.util.*

class ConfigurationEditorController : FxController() {

    @Autowired
    private lateinit var pluginConfigurationController: PluginConfigurationController

    @Autowired
    private lateinit var configurationEditorFieldController: ConfigurationEditorFieldController

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var releaseRepository: ReleaseRepository

    @Autowired
    private lateinit var detailsRepository: DetailsRepository

    @Autowired
    private lateinit var fileService: FileService

    @Autowired
    private lateinit var files: ApplicationFiles

    @FXML
    private lateinit var groupComboBox: ComboBox<Group>

    @FXML
    private lateinit var releaseComboBox: ComboBox<Release>

    @FXML
    private lateinit var removeGroupButton: VBox

    @FXML
    private lateinit var addReleaseButton: VBox

    @FXML
    private lateinit var removeReleaseButton: VBox

    @FXML
    private lateinit var acceptButton: Button

    @FXML
    private lateinit var getScriptButton: Button

    @FXML
    private lateinit var description: TextArea

    @FXML
    private lateinit var imageView: ImageView

    @FXML
    private lateinit var addImageButton: VBox

    @FXML
    private lateinit var removeImageButton: VBox

    @FXML
    private lateinit var openEditPaneButton: VBox

    @FXML
    private lateinit var closeEditPaneButton: VBox

    @FXML
    private lateinit var detailsListView: ListView<DetailsListViewLine>

    @FXML
    private lateinit var newDetailsListView: ListView<NewDetailsListViewLine>

    @FXML
    private lateinit var footer: VBox

    @FXML
    private lateinit var addFilesBox: VBox

    @FXML
    private lateinit var gamePathLabelTop: Label

    @FXML
    private lateinit var optionalPathLabelTop: Label

    @FXML
    private lateinit var gamePathLabelBottom: Label

    @FXML
    private lateinit var optionalPathLabelBottom: Label

    @FXML
    private lateinit var gamePrefix: TextField

    @FXML
    private lateinit var optionalPrefix: TextField

    private lateinit var removeGroups: MutableList<Group>
    private lateinit var removeReleases: MutableList<Release>
    private lateinit var removeDetails: MutableList<Details>
    private var editPanelShow = false

    override fun init() {
        groupComboBox.converter = object : StringConverter<Group?>() {
            override fun toString(group: Group?) = group?.value ?: ""

            override fun fromString(string: String): Group? = null
        }

        acceptButton.disabledProperty().addListener { _, _, enable: Boolean? ->
            enable?.also { getScriptButton.isDisable = !enable }
        }

        stage.onShowing = EventHandler {
            removeGroups = mutableListOf()
            removeReleases = mutableListOf()
            removeDetails = mutableListOf()
            setGroupToCombobox()
            closeEditPanel()
            acceptButton.isDisable = true
            var path = files.gameFolder.absolutePath + File.separator
            gamePathLabelTop.text = path
            gamePathLabelBottom.text = path
            var tooltip = Tooltip(path)
            tooltip.showDelay = Duration.seconds(0.1)
            gamePathLabelTop.tooltip = tooltip
            gamePathLabelBottom.tooltip = tooltip
            path = files.optional.absolutePath + File.separator
            optionalPathLabelTop.text = path
            optionalPathLabelBottom.text = path
            tooltip = Tooltip(path)
            tooltip.showDelay = Duration.seconds(0.1)
            optionalPathLabelTop.tooltip = tooltip
            optionalPathLabelBottom.tooltip = tooltip
        }

        groupComboBox.selectionModel.selectedItemProperty()
            .addListener { _: ObservableValue<out Group?>?, _: Group?, newValue: Group? ->
                removeGroupButton.isDisable = groupComboBox.items.size == 0
                addReleaseButton.isDisable = newValue == null
                if (newValue != null) {
                    val releases: List<Release> = groupComboBox.selectionModel.selectedItem.releases
                    if (releases != null) {
                        releases.sortedBy { it.value }
                        releaseComboBox.items.clear()
                        releaseComboBox.items.addAll(releases)
                        if (releaseComboBox.items.size > 0) {
                            releaseComboBox.selectionModel.select(0)
                        }
                        return@addListener
                    }
                }
                releaseComboBox.items.clear()
            }

        groupComboBox.onMouseClicked = EventHandler { event: MouseEvent ->
            if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                val group = groupComboBox.selectionModel.selectedItem
                if (group != null) {
                    val value = configurationEditorFieldController.showAndWait(group.value)
                    group.value = value
                    val release = releaseComboBox.selectionModel.selectedItem
                    groupComboBox.selectionModel.clearSelection()
                    groupComboBox.selectionModel.select(group)
                    releaseComboBox.selectionModel.select(release)
                }
            }
        }

        releaseComboBox.onMouseClicked = EventHandler { event: MouseEvent ->
            if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
                val release = releaseComboBox.selectionModel.selectedItem
                if (release != null) {
                    val value = configurationEditorFieldController.showAndWait(release.value)
                    release.value = value
                    releaseComboBox.selectionModel.clearSelection()
                    releaseComboBox.selectionModel.select(release)
                }
            }
        }

        releaseComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue: Release? ->
            removeReleaseButton.isDisable = releaseComboBox.items.size == 0
            closeEditPanel()
            description.isDisable = newValue == null
            addImageButton.isDisable = newValue == null
            openEditPaneButton.isDisable = newValue == null
            if (newValue != null) {
                description.text = newValue.description
                setImage(newValue.image)
                detailsListView.items.clear()
                val details: List<Details> = newValue.details
                if (details.isNotEmpty()) {
                    details.forEach { DetailsListViewLine(it) }
                } else {
                    detailsListView.items.clear()
                }
            } else {
                description.text = ""
                imageView.image = null
                detailsListView.items.clear()
            }
        }

        imageView.imageProperty().addListener { _, _, _ -> removeImageButton.isDisable = imageView.image == null }

        description.textProperty().addListener { _, _, newValue: String? ->
            if (newValue != null) {
                val selectedItem = releaseComboBox.selectionModel.selectedItem
                if (selectedItem != null) {
                    selectedItem.description = newValue
                }
            }
        }

        gamePrefix.textProperty().addListener { _, _, newValue: String? ->
            if (newValue != null) {
                newDetailsListView.items.forEach { obj: NewDetailsListViewLine -> obj.setGamePath() }
            }
        }

        optionalPrefix.textProperty().addListener { _, _, newValue: String? ->
            log().error(newValue)
            if (newValue != null) {
                newDetailsListView.items.forEach { obj: NewDetailsListViewLine -> obj.setOptionsPath() }
            }
        }

        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event: KeyEvent ->
            if (event.code == KeyCode.ESCAPE) {
                hide()
            }
        }

        stage.onHidden = EventHandler { pluginConfigurationController.show() }
    }

    fun addGroup() {
        if (groupComboBox.items.stream().noneMatch { group -> group.value.isBlank() }) {
            val text = configurationEditorFieldController.showAndWait("")
            if (!text.isBlank()) {
                Group(text, ArrayList()).also {
                    groupComboBox.items.add(it)
                    groupComboBox.selectionModel.select(it)
                    acceptButton.isDisable = false
                }
            }
        }
    }

    fun removeGroup() {
        groupComboBox.selectionModel.selectedItem?.also {
            removeGroups.add(it)
            groupComboBox.items.remove(it)
            if (groupComboBox.items.isNotEmpty()) {
                groupComboBox.selectionModel.select(0)
            }
            acceptButton.isDisable = false
        }
    }

    fun addRelease() {
        if (releaseComboBox.items.stream().noneMatch { release -> release.value.isBlank() }) {
            val text = configurationEditorFieldController.showAndWait("")
            if (!text.isBlank()) {
                groupComboBox.selectionModel.selectedItem.releases.add(
                    Release(
                        value = text,
                        applied = false,
                        group = groupComboBox.selectionModel.selectedItem
                    ).also {
                        releaseComboBox.items.add(it)
                        releaseComboBox.selectionModel.select(it)
                        acceptButton.isDisable = false
                    }
                )
            }
        }
    }

    fun removeRelease() {
        releaseComboBox.selectionModel.selectedItem?.also {
            removeReleases.add(it)
            groupComboBox.selectionModel.selectedItem.releases.remove(it)
            releaseComboBox.items.remove(it)
            if (releaseComboBox.items.isNotEmpty()) {
                releaseComboBox.selectionModel.select(0)
            }
            acceptButton.isDisable = false
        }
    }

    fun addImage() {
        val file = fileService.openFileChooser(files.optional, stage)
        if (file.absolutePath.contains(files.optional.absolutePath)) {
            val optionalPath = file.absolutePath.replace(files.optional.absolutePath + File.separator, "")
            releaseComboBox.selectionModel.selectedItem.image = optionalPath
            setImage(optionalPath)
            acceptButton.isDisable = false
        }
    }

    fun removeImage() {
        releaseComboBox.selectionModel.selectedItem.image = null
        imageView.image = null
        acceptButton.isDisable = false
    }

    private fun setGroupToCombobox() {
        val groups = groupService.getAllWithDetails()
        groups.sortedBy { it.value }
        groupComboBox.items.clear()
        groupComboBox.items.addAll(groups)
        if (groupComboBox.items.isNotEmpty()) {
            groupComboBox.selectionModel.select(0)
        }
    }

    private fun setImage(imagePath: String?) {
        try {
            if (imagePath != null) {
                val path = File(files.optional.absolutePath + File.separator + imagePath).toURI().toURL().toString()
                imageView.image = Image(path)
            } else {
                imageView.image = null
            }
        } catch (e: MalformedURLException) {
            log().error(String.format("Can't use image for %s\n", imagePath), e)
        }
    }

    fun saveChanges() {
        groupService.removeAll(removeGroups)
        releaseRepository.deleteAll(removeReleases)
        detailsRepository.deleteAll(removeDetails)
        groupService.saveAll(groupComboBox.items)
        acceptButton.isDisable = true
    }

    fun getScript() {
        val groups: List<Group> = groupService.getAllWithDetails()
        val file = fileService.openSaveFileChooser(files.optional, files.schema.name, stage)
        fileService.createSchemaFile(groups, file)
    }

    fun openEditPanelAndSaveChanges() {
        if (editPanelShow) {
            newDetailsListView.items
                .map { it.getDetails() }
                .onEach { details -> DetailsListViewLine(details) }
                .forEach { details -> details.release!!.details.add(details) }
            closeEditPanel()
            acceptButton.isDisable = false
        } else {
            openEditPanel()
        }
    }

    fun closeEditPanel() {
        footer.prefHeight = 2.0
        addFilesBox.isVisible = false
        editPanelShow = false
        closeEditPaneButton.isDisable = true
        newDetailsListView.items.clear()
    }

    private fun openEditPanel() {
        footer.prefHeight = 150.0
        addFilesBox.isVisible = true
        editPanelShow = true
        closeEditPaneButton.isDisable = false
    }

    fun getFilesFromFolder() {
        val file = fileService.openDirectoryChooser(files.optional, stage)
        if (file.exists() && file.isDirectory) {
            val files: List<File> = file.listAllFiles()
            files.map { Details(releaseComboBox.selectionModel.selectedItem, it.absolutePath, it.absolutePath) }
                .forEach { details -> NewDetailsListViewLine(details) }
        }
    }

    fun getFiles() {
        val files = fileService.openFilesChooser(files.optional, stage)
        files.filter { it.isFile }
            .map {
                Details(
                    release = releaseComboBox.selectionModel.selectedItem,
                    storagePath = it.absolutePath,
                    gamePath = it.absolutePath
                )
            }
            .forEach { details -> NewDetailsListViewLine(details) }
    }

    abstract inner class ListViewLine(
        private val details: Details
    ) : HBox() {

        private val buttonWrapper = HBox()
        var gamePath = TextField()
        var optionalPath = TextField()
        var removeButton: Button

        private fun createRemoveButton(): Button {
            val base = VBox()
            base.styleClass.add("button59")
            val buttonShadow = VBox()
            buttonShadow.styleClass.add("buttonShadow")
            VBox.setMargin(buttonShadow, Insets(0.0, 0.0, -35.0, 0.0))
            base.children.add(buttonShadow)
            val buttonBackground = VBox()
            buttonBackground.styleClass.add("buttonBackground")
            base.children.add(buttonBackground)
            val buttonHover = VBox()
            buttonHover.styleClass.add("buttonHover")
            buttonBackground.children.add(buttonHover)
            val button = Button("-")
            buttonHover.children.add(button)
            buttonWrapper.children.add(base)
            return button
        }

        fun getDetails(): Details {
            details.gamePath = gamePath.text
            details.storagePath = optionalPath.text
            return details
        }

        init {
            val leftLineWrapper = HBox()
            children.add(leftLineWrapper)
            val rightLineWrapper = HBox()
            children.add(rightLineWrapper)
            children.add(buttonWrapper)
            styleClass.add("line")
            leftLineWrapper.styleClass.add("left")
            rightLineWrapper.styleClass.add("middle")
            buttonWrapper.styleClass.add("right")
            leftLineWrapper.children.add(gamePath)
            rightLineWrapper.children.add(optionalPath)
            gamePath.text = details.gamePath
            optionalPath.text = details.storagePath
            removeButton = createRemoveButton()
        }
    }

    inner class DetailsListViewLine(details: Details) : ListViewLine(details) {
        init {
            removeButton.onAction = EventHandler {
                detailsListView.items.remove(this)
                removeDetails.add(details)
                details.release!!.details.remove(details)
            }
            detailsListView.items.add(this)
        }
    }

    inner class NewDetailsListViewLine(details: Details) : ListViewLine(details) {
        fun setOptionsPath() {
            val path: String = if (optionalPrefix.text.isBlank()) {
                files.optional.absolutePath + File.separator
            } else {
                files.optional.absolutePath + File.separator + optionalPrefix.text + File.separator
            }
            optionalPath.text = getDetails().storagePath.replace(path, "")
            setGamePath()
        }

        fun setGamePath() {
            if (gamePrefix.text.isBlank()) {
                gamePath.text = optionalPath.text
            } else {
                gamePath.text = gamePrefix.text + File.separator + optionalPath.text
            }
        }

        init {
            removeButton.onAction = EventHandler { newDetailsListView.items.remove(this) }
            newDetailsListView.items.add(this)
            gamePath.isEditable = true
            optionalPath.isEditable = true
            setOptionsPath()
        }
    }
}