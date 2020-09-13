package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable

interface ProgressBar : Loggable {

    var progressBar: HBox

    var progressBarDecoration: VBox

    var description: Label

    var progress: Label

    var closeButton: VBox

    fun updateProgress(workDone: Int, max: Int) = updateProgress(workDone.toLong(), max.toLong())

    fun updateProgress(workDone: Long, max: Long) {
        CoroutineScope(Dispatchers.JavaFx).launch {
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
    }


    fun setDescription(text: String) {
        CoroutineScope(Dispatchers.JavaFx).launch { description.text = text }
    }

    fun setCloseButtonVisible(visible: Boolean) {
        CoroutineScope(Dispatchers.JavaFx).launch { closeButton.isVisible = visible }
    }

    private fun disable() {
        CoroutineScope(Dispatchers.JavaFx).launch {
            setProgressBarStyle(DISABLE, "")
            setCloseButtonVisible(true)
        }
    }

    private fun enable() {
        CoroutineScope(Dispatchers.JavaFx).launch {
            setProgressBarStyle(ENABLE, "1%")
            progressBar.prefWidth = progressBarMinWidth
        }
    }

    private fun full() {
        CoroutineScope(Dispatchers.JavaFx).launch { setProgressBarStyle(FULL, "100%") }
    }

    private fun empty() {
        CoroutineScope(Dispatchers.JavaFx).launch { setProgressBarStyle(EMPTY, "0%") }
    }

    private fun setProgressBarStyle(style: String, text: String) {
        progress.text = text
        progressBarDecoration.styleClass.removeAll(*STYLES)
        progressBarDecoration.styleClass.add(style)
    }

    val progressBarMinWidth: Double

    val progressBarMaxWidth: Double

    companion object {
        private const val FULL = "full"
        private const val ENABLE = "enable"
        private const val DISABLE = "disable"
        private const val EMPTY = "empty"
        private val STYLES = arrayOf(FULL, ENABLE, DISABLE, EMPTY)
    }
}