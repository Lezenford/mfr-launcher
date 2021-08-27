package ru.fullrest.mfr.launcher.javafx.controller

import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.RadioButton
import javafx.scene.control.TextArea
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.ObjectFactory
import org.springframework.stereotype.Component
import ru.fullrest.mfr.common.api.ContentType
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.common.extensions.toPath
import ru.fullrest.mfr.javafx.component.FxController
import ru.fullrest.mfr.launcher.component.EventPublisher
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.javafx.TaskFactory
import ru.fullrest.mfr.launcher.javafx.component.DownloadButton
import ru.fullrest.mfr.launcher.javafx.component.DownloadExtraButton
import ru.fullrest.mfr.launcher.javafx.component.DownloadOptionButton
import ru.fullrest.mfr.launcher.javafx.component.LauncherProgressBar
import ru.fullrest.mfr.launcher.model.entity.Extra
import ru.fullrest.mfr.launcher.model.entity.Option
import ru.fullrest.mfr.launcher.model.entity.Section
import ru.fullrest.mfr.launcher.service.ExtraService
import ru.fullrest.mfr.launcher.service.RestTemplateService
import ru.fullrest.mfr.launcher.service.SectionService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.inputStream

@Component
class GameController(
    private val applicationProperties: ApplicationProperties,
    private val sectionService: SectionService,
    private val extraService: ExtraService,
    private val publisher: EventPublisher,
    private val restTemplateService: RestTemplateService,
    private val taskFactory: TaskFactory,
    private val notificationControllerFactory: ObjectFactory<NotificationController>
) : FxController(source = "fxml/game-options.fxml") {
    private val sectionsContainer: VBox by fxml()
    private val extraContent: VBox by fxml()
    private val optionDescription: TextArea by fxml()
    private val optionsContainer: VBox by fxml()
    private val imageContainer: HBox by fxml()
    private val applyButton: Button by fxml()

    private val progressBar = LauncherProgressBar(fxmlLoader)

    private val sectionToggleGroup = ToggleGroup()
    private val lock = AtomicBoolean(false)

    override fun onShowing() {
        sectionsContainer.children.addAll(sectionService.findAllWithDetails()
            .sortedBy { it.name }.map { SectionRow(it) })
        sectionsContainer.children.find { it is SectionRow }?.let { it as SectionRow }?.button
            ?.also { it.toggleGroup.selectToggle(it) }
        extraContent.children.addAll(extraService.findAll().sortedBy { it.name }.map { ExtraContentRow(it) })
        progressBar.hide()
        applyButton.isDisable = true
    }

    override fun onHiding() {
        publisher.sendShowRequest(LauncherController::class)
        sectionsContainer.children.filterIsInstance(SectionRow::class.java).forEach { it.button.toggleGroup = null }
        sectionsContainer.children.clear()
        extraContent.children.clear()
    }

    fun applyChanges() {
        val actualSections = sectionsContainer.children.filterIsInstance(SectionRow::class.java)
            .map { it.section }
        val savedSections = sectionService.findAllWithDetails()
        val notDownloadedOptions = mutableListOf<String>()
        val listOptionsForApply = actualSections.mapNotNull { section ->
            section.options.find { it.applied }?.let {
                savedSections.first { it.name == section.name }
                    .let { savedSection -> savedSection.options.find { it.applied } to it }
            }
        }.filter { (currentOption, newOption) -> (currentOption?.id != newOption.id) }
            .filter { pair -> pair.second.section.downloaded.also { if (it.not()) notDownloadedOptions.add(pair.second.section.name) } }

        if (notDownloadedOptions.isNotEmpty()) {
            notificationControllerFactory.`object`.info(
                description = """Часть опций не будет применена, т.к. пакет опций не был загружен.
                    |Пропущены следующие опции: ${notDownloadedOptions.joinToString(", ")}""".trimMargin()
            )
        }

        launch(Dispatchers.Default) {
            withContext(Dispatchers.JavaFx) { applyButton.isDisable = true }
            taskFactory.applyOptionsTask().execute(progressBar, listOptionsForApply)
            progressBar.hide()
        }
    }

    private suspend fun downloadOption(type: ContentType, name: String, button: DownloadButton): Boolean {
        if (lock.compareAndSet(false, true)) {
            try {
                withContext(Dispatchers.JavaFx) { button.isDisable = true }
                try {
                    val files = restTemplateService.content().categories
                        .first { it.type == type }.items.find { it.name == name }?.files
                        ?.associateBy { it.id }?.toMutableMap()
                        ?: mutableMapOf()
                    taskFactory.fileDownloadTask().execute(files, progressBar)
                } finally {
                    withContext(Dispatchers.JavaFx) { button.isDisable = false }
                    progressBar.hide()
                }
                return true
            } finally {
                lock.set(false)
            }
        }
        return false
    }

    private inner class ExtraContentRow(extra: Extra) : HBox() {
        init {
            ToggleButton(extra.name).also {
                children.addAll(
                    HBox().apply {
                        children.add(it)
                        setHgrow(this, Priority.ALWAYS)
                        alignment = Pos.CENTER
                    },
                    DownloadExtraButton(extra, applicationProperties, extraService) {
                        launch(Dispatchers.Default) {
                            if (downloadOption(ContentType.EXTRA, extra.name, this@DownloadExtraButton)) {
                                setStatus(DownloadButton.Status.REMOVE)
                                extra.downloaded = true
                                extraService.save(extra)
                            }
                        }
                    })
                it.selectedProperty().addListener { _, oldValue, newValue ->
                    //TODO добавить обновление ini
                }
            }
            alignmentProperty().set(Pos.CENTER)
        }
    }

    private inner class SectionRow(
        val section: Section
    ) : HBox() {
        val button = ToggleButton(section.name).also {
            it.toggleGroup = sectionToggleGroup
            children.addAll(
                HBox().apply {
                    children.add(it)
                    setHgrow(this, Priority.ALWAYS)
                    alignment = Pos.CENTER
                },
                DownloadOptionButton(section, applicationProperties, sectionService) {
                    launch(Dispatchers.Default) {
                        if (downloadOption(ContentType.OPTIONAL, section.name, this@DownloadOptionButton)) {
                            setStatus(DownloadButton.Status.REMOVE)
                            section.downloaded = true
                            sectionService.save(section)
                        }
                    }
                })
            it.selectedProperty().addListener { _, oldValue, newValue ->
                if (oldValue != newValue && newValue) {
                    fillInformation()
                }
            }
        }

        fun fillInformation() {
            val group = ToggleGroup().also { group ->
                group.selectedToggleProperty().addListener { _, oldValue, newValue ->
                    imageContainer.children.clear()
                    newValue?.also { value ->
                        oldValue?.let { it.userData as Option }?.applied = false
                        val option = value.userData as Option
                        optionDescription.text = option.description
                        option.applied = true
                        option.image?.also {
                            imageContainer.children.add(
                                ImageView().apply {
                                    image =
                                        Image(applicationProperties.gameFolder.resolve(it.toPath()).inputStream())
                                }
                            )
                        }
                    } ?: kotlin.run {
                        optionDescription.text = ""
                    }
                }
            }
            optionsContainer.children.clear()
            imageContainer.children.clear()
            optionDescription.text = ""
            optionsContainer.children.addAll(
                section.options.sortedBy { it.name }.map { option ->
                    HBox().apply {
                        alignment = Pos.CENTER_LEFT
                        children.add(
                            RadioButton(option.name).also {
                                it.userData = option
                                it.toggleGroup = group
                                if (option.applied) {
                                    it.toggleGroup.selectToggle(it)
                                }
                                it.onMouseClicked = EventHandler {
                                    applyButton.isDisable = false
                                }
                            }
                        )
                    }
                }
            )
        }

        init {
            this.alignmentProperty().set(Pos.CENTER)
        }
    }

    companion object {
        private val log by Logger()
    }
}