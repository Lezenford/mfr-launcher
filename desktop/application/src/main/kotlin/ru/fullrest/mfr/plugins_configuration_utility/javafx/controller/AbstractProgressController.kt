package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxTask

abstract class AbstractProgressController : FxController() {

    @FXML
    protected lateinit var progressBar: HBox

    @FXML
    protected lateinit var progressBarDecoration: VBox

    @FXML
    protected lateinit var description: Label

    @FXML
    protected lateinit var progress: Label

    @FXML
    protected lateinit var closeButton: VBox

    fun updateProgress(workDone: Int, max: Int) = updateProgress(workDone.toLong(), max.toLong())

    fun updateProgress(workDone: Long, max: Long) = launch {
        if (workDone < 0 || max < 0) {
            log().error("Count must be 0 or higher")
            setDescription("Ошибка!")
            disable()
        }
        val percents = (workDone.toDouble() / max * 100).toInt()
        if (percents < 0 || percents > 100) {
            log().error("Percents must be from 0 to 100. Current percents: $percents workDone: $workDone max: $max")
            setDescription("Ошибка!")
            disable()
        } else {
            if (percents == 0) {
                empty()
            } else {
                if (percents < 100) {
                    if (!progressBarDecoration.styleClass.contains(ENABLE)) {
                        enable()
                    }
                    val width = (progressBarMaxWidth - progressBarMinWidth) * percents / 100 + progressBarMinWidth
                    progress.text = "$percents%"
                    progressBar.prefWidth = width
                } else {
                    full()
                }
            }
        }
    }


    fun setDescription(text: String) = launch {
        description.text = text
    }

    fun setCloseButtonVisible(visible: Boolean) = launch {
        closeButton.isVisible = visible
    }

    private fun disable() = launch {
        setProgressBarStyle(DISABLE, "")
        setCloseButtonVisible(true)
    }

    private fun enable() = launch {
        setProgressBarStyle(ENABLE, "1%")
        progressBar.prefWidth = progressBarMinWidth
        setCloseButtonVisible(false)
    }

    private fun full() = launch {
        setProgressBarStyle(FULL, "100%")
    }

    private fun empty() = launch {
        setProgressBarStyle(EMPTY, "0%")
    }

    private fun setProgressBarStyle(style: String, text: String) {
        progress.text = text
        progressBarDecoration.styleClass.removeAll(*STYLES)
        progressBarDecoration.styleClass.add(style)
    }

    suspend fun <T> runAsync(task: FxTask<T>) = CoroutineScope(Dispatchers.Default).async {
        task.progressController = this@AbstractProgressController
        task.process()
    }

    suspend fun <T> runJob(task: FxTask<T>) = CoroutineScope(Dispatchers.Default).launch {
        task.progressController = this@AbstractProgressController
        task.process()
    }

    protected abstract val progressBarMinWidth: Double

    protected abstract val progressBarMaxWidth: Double

    companion object {
        private const val FULL = "full"
        private const val ENABLE = "enable"
        private const val DISABLE = "disable"
        private const val EMPTY = "empty"
        private val STYLES = arrayOf(FULL, ENABLE, DISABLE, EMPTY)
    }
}