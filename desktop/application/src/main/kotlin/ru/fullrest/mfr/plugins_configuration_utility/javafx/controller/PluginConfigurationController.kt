package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.*
import javafx.scene.layout.VBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Autowired
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.javafx.TaskFactory
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.model.entity.Release
import ru.fullrest.mfr.plugins_configuration_utility.service.GroupService
import java.io.File
import java.net.MalformedURLException
import java.util.*
import java.util.function.Consumer

class PluginConfigurationController : FxController() {

    @Autowired
    private lateinit var taskFactory: TaskFactory

    @Autowired
    private lateinit var launcherController: LauncherController

    @Autowired
    private lateinit var gameUpdateController: GameUpdateController

    @Autowired
    private lateinit var configurationEditorController: ConfigurationEditorController

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var files: ApplicationFiles

    @FXML
    private lateinit var acceptButton: Button

    @FXML
    private lateinit var scriptEditorButton: VBox

    @FXML
    private lateinit var groupVBox: VBox

    @FXML
    private lateinit var releaseButtonList: ListView<RadioButton>

    @FXML
    private lateinit var imageView: ImageView

    @FXML
    private lateinit var description: TextArea

    private lateinit var groupButtons: ToggleGroup

    @Suppress("UNCHECKED_CAST")
    override fun init() {
        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event: KeyEvent ->
            if (event.code == KeyCode.ESCAPE) {
                close()
            }
        }

        val keyCombination: KeyCombination = KeyCodeCombination(KeyCode.E, KeyCombination.CONTROL_DOWN)
        stage.addEventHandler(KeyEvent.KEY_PRESSED) { event ->
            if (keyCombination.match(event)) {
                scriptEditorButton.isVisible = true
            }
        }

        stage.onShowing = EventHandler {
            acceptButton.isDisable = true
            //add groups
            groupVBox.children.clear()
            val groups = groupService.getAll()
            groupButtons = ToggleGroup()
            groups.forEach { group ->
                val button = ToggleButton()
                button.toggleGroup = groupButtons
                button.text = group.value
                button.userData = group
                groupVBox.children.add(button)
                val releases: MutableList<RadioButton> = ArrayList()
                button.userData = releases
                val releaseToggleGroup = ToggleGroup()
                group.releases.forEach { release ->
                    val releaseButton = RadioButton()
                    releaseButton.text = release.value
                    releaseButton.toggleGroup = releaseToggleGroup
                    releaseButton.isSelected = release.applied
                    releaseButton.userData = release
                    releases.add(releaseButton)
                    releaseButton.onMouseClicked = EventHandler {
                        if (it.button == MouseButton.PRIMARY) {
                            setRelease(release)
                            acceptButton.isDisable = false
                        }
                    }
                }
            }
            groupButtons.selectedToggleProperty().addListener { _, oldValue: Toggle?, newValue: Toggle? ->
                newValue?.also {
                    releaseButtonList.items.clear()
                    description.text = ""
                    imageView.image = null
                    for (button in newValue.userData as List<RadioButton>) {
                        releaseButtonList.items.add(button)
                        if (button.isSelected) {
                            setRelease(button.userData as Release)
                        }
                    }
                } ?: kotlin.run { oldValue?.isSelected = true } //Do not allow to disable all buttons
            }
            if (groupButtons.toggles.size > 0) {
                groupButtons.toggles[0].isSelected = true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun accept() = CoroutineScope(Dispatchers.JavaFx).launch {
        val releasesForApply: MutableList<Release> = ArrayList()
        groupButtons.toggles.forEach(Consumer { groupButton ->
            val releaseButtons =
                groupButton.userData as List<RadioButton>
            releaseButtons.forEach(Consumer { releaseButton: RadioButton ->
                val release = releaseButton.userData as Release
                if (releaseButton.isSelected && !release.applied) {
                    releasesForApply.add(release)
                }
            })
        })
        hide()
        gameUpdateController.runJob(taskFactory.getPluginTask().also { it.releases = releasesForApply })
        gameUpdateController.showAndWait()
        show()
    }

    fun openScriptEditor() {
        configurationEditorController.show()
        hide()
    }

    private fun setRelease(release: Release) {
        description.text = release.description
        if (release.image != null && !release.image!!.isBlank()) {
            var path: String? = null
            try {
                path = File(files.optional.path + File.separator + release.image).toURI().toURL()
                    .toString()
            } catch (e: MalformedURLException) {
                log().error("Can't use image for ${release.image}\n", e)
            }
            val image: Image
            if (path != null) {
                image = Image(path)
                imageView.image = image
            } else {
                imageView.image = null
            }
        }
    }

    fun close() = launch {
        hide()
        launcherController.show()
    }
}