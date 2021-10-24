package ru.fullrest.mfr.launcher.javafx.controller

import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.RadioButton
import javafx.scene.control.TextArea
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import kotlinx.coroutines.CoroutineScope
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
import ru.fullrest.mfr.javafx.extensions.withFx
import ru.fullrest.mfr.launcher.component.EventPublisher
import ru.fullrest.mfr.launcher.config.properties.ApplicationProperties
import ru.fullrest.mfr.launcher.javafx.TaskFactory
import ru.fullrest.mfr.launcher.javafx.component.LauncherProgressBar
import ru.fullrest.mfr.launcher.model.entity.Extra
import ru.fullrest.mfr.launcher.model.entity.Option
import ru.fullrest.mfr.launcher.model.entity.Section
import ru.fullrest.mfr.launcher.service.ExtraService
import ru.fullrest.mfr.launcher.service.RestTemplateService
import ru.fullrest.mfr.launcher.service.SectionService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.deleteIfExists
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
    private val optionsForApply: MutableSet<Int> = mutableSetOf()

    private val initialize by lazy {
        sectionsContainer.children.addAll(sectionService.findAllWithDetails()
            .sortedBy { it.name }.map { SectionRow(it) })
        sectionsContainer.children.find { it is SectionRow }?.let { it as SectionRow }?.button
            ?.also { it.toggleGroup.selectToggle(it) }
        extraContent.children.addAll(extraService.findAll().sortedBy { it.name }.map { ExtraContentRow(it) })
        progressBar.hide()
        applyButton.isDisable = true
    }

    override fun onShowing() {
        initialize
        optionsForApply.clear()
    }

    override fun onHiding() {
        publisher.sendShowRequest(LauncherController::class)
    }

    fun applyChanges() {
        val savedSections = sectionService.findAllWithDetails()
        val notDownloadedOptions = mutableListOf<String>()
        val listOptionsForApply =
            savedSections.filter { section -> section.options.any { optionsForApply.contains(it.id) } }
                .filter {
                    it.downloaded.apply {
                        if (not()) {
                            notDownloadedOptions.add(it.name)
                        }
                    }
                }
                .map { section ->
                    section.options.find { it.applied } to section.options.first { optionsForApply.contains(it.id) }
                }



        launch(Dispatchers.Default) {
            withContext(Dispatchers.JavaFx) { applyButton.isDisable = true }
            optionsForApply.clear()
            withFx {
                if (notDownloadedOptions.isNotEmpty()) {
                    notificationControllerFactory.`object`.info(
                        description = """Часть опций не будет применена, т.к. пакет опций не был загружен.
                    |Пропущены следующие опции: ${notDownloadedOptions.joinToString(", ")}""".trimMargin()
                    )
                }
            }
            taskFactory.applyOptionsTask().execute(progressBar, listOptionsForApply)
            progressBar.hide()
        }
    }

    private fun fillInformation(section: Section) {
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
                                image = Image(applicationProperties.gameFolder.resolve(it.toPath()).inputStream())
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
                                optionsForApply.add(option.id)
                            }
                        }
                    )
                }
            }
        )
    }

    private abstract inner class Row<T>(val value: T) : HBox() {
        protected abstract val contentType: ContentType
        protected abstract val name: String
        protected abstract val filePaths: List<String>

        init {
            alignmentProperty().set(Pos.CENTER)
        }

        protected abstract inner class RowButton(private var status: ButtonStatus) : Button() {
            init {
                when (status) {
                    ButtonStatus.DOWNLOAD -> styleClass.add(DOWNLOAD_BUTTON_STYLE)
                    ButtonStatus.REMOVE -> styleClass.add(REMOVE_BUTTON_STYLE)
                }
                onMouseClicked = EventHandler {
                    CoroutineScope(Dispatchers.Default).launch { onClick() }
                }
            }

            private suspend fun onClick() {
                withFx { isDisable = true }
                when (status) {
                    ButtonStatus.REMOVE -> {
                        if (remove()) {
                            styleClass.removeAll(REMOVE_BUTTON_STYLE)
                            styleClass.add(DOWNLOAD_BUTTON_STYLE)
                            status = ButtonStatus.DOWNLOAD
                        }
                    }
                    ButtonStatus.DOWNLOAD -> {
                        if (download()) {
                            styleClass.removeAll(DOWNLOAD_BUTTON_STYLE)
                            styleClass.add(REMOVE_BUTTON_STYLE)
                            status = ButtonStatus.REMOVE
                        }
                    }
                }
                withFx { isDisable = false }
            }

            protected suspend fun download(): Boolean {
                if (lock.compareAndSet(false, true)) {
                    try {
                        progressBar.updateProgress(0)
                        try {
                            val files = restTemplateService.content().categories
                                .first { it.type == contentType }.items.find { it.name == name }?.files
                                ?.associateBy { it.id }?.toMutableMap()
                                ?: mutableMapOf()
                            taskFactory.fileDownloadTask().execute(files, progressBar)
                            save()
                        } finally {
                            progressBar.hide()
                        }
                        return true
                    } finally {
                        lock.set(false)
                    }
                }
                return false
            }

            protected suspend fun remove(): Boolean {
                if (lock.compareAndSet(false, true)) {
                    try {
                        try {
                            withFx { progressBar.updateProgress(0) }
                            filePaths.forEachIndexed { index, path ->
                                applicationProperties.gameFolder.resolve(path.toPath()).deleteIfExists()
                                withFx { progressBar.updateProgress(index.toLong(), filePaths.size.toLong()) }
                            }
                            save()
                        } finally {
                            progressBar.hide()
                        }
                        return true
                    } finally {
                        lock.set(false)
                    }
                }
                return false
            }

            protected abstract fun save()
        }
    }

    private inner class ExtraContentRow(extra: Extra) : Row<Extra>(extra) {
        override val contentType: ContentType = ContentType.EXTRA
        override val name: String = extra.name
        override val filePaths: List<String> = extra.files.map { it.path }

        init {
            children.addAll(
                HBox().apply {
                    children.add(Label(extra.name).also { it.styleClass.add(EXTRA_LABEL_STYLE) })
                    setHgrow(this, Priority.ALWAYS)
                    alignment = Pos.CENTER
                },
                ExtraRowButton(if (extra.downloaded) ButtonStatus.REMOVE else ButtonStatus.DOWNLOAD)
            )
        }

        private inner class ExtraRowButton(status: ButtonStatus) : RowButton(status) {
            override fun save() {
                value.downloaded = value.downloaded.not()
                extraService.save(value)
            }
        }
    }

    private inner class SectionRow(
        section: Section
    ) : Row<Section>(section) {
        override val contentType: ContentType = ContentType.OPTIONAL
        override val name: String = section.name
        override val filePaths: List<String> = section.options.flatMap { it.items }.map { it.gamePath }

        val button = ToggleButton(section.name).also {
            it.toggleGroup = sectionToggleGroup
            children.addAll(
                HBox().apply {
                    children.add(it)
                    setHgrow(this, Priority.ALWAYS)
                    alignment = Pos.CENTER
                },
                SectionRowButton(if (section.downloaded) ButtonStatus.REMOVE else ButtonStatus.DOWNLOAD)
            )
            it.selectedProperty().addListener { _, oldValue, newValue ->
                if (oldValue != newValue && newValue) {
                    fillInformation(section)
                }
            }
        }

        private inner class SectionRowButton(status: ButtonStatus) : RowButton(status) {
            override fun save() {
                value.downloaded = value.downloaded.not()
                sectionService.save(value)
            }
        }
    }

    private enum class ButtonStatus {
        DOWNLOAD, REMOVE
    }

    companion object {
        private val log by Logger()

        private const val EXTRA_LABEL_STYLE = "option_label"
        private const val REMOVE_BUTTON_STYLE = "remove"
        private const val DOWNLOAD_BUTTON_STYLE = "download"
    }
}