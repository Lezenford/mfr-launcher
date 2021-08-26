package ru.fullrest.mfr.javafx.component

import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import ru.fullrest.mfr.common.extensions.Logger

abstract class ProgressBar(override val fxmlLoader: FXMLLoader) : FXMLComponent(), CoroutineScope {
    override val coroutineContext = Dispatchers.JavaFx

    private val progressBar: VBox by fxml()
    private val track: HBox by fxml()
    private val percents: Label by fxml()
    private val description: Label by fxml()
    protected abstract val maxLength: Int

    fun hide() {
        launch {
            progressBar.styleClass.removeAll(STYLES)
            progressBar.styleClass.add(HIDE)
        }
    }

    fun disable() {
        launch {
            progressBar.styleClass.removeAll(STYLES)
            progressBar.styleClass.addAll(DISABLE)
        }
    }

    fun updateDescription(text: String) {
        launch {
            description.text = text
        }
    }

    fun updateProgress(current: Long, max: Long) {
        if (max == 0L) {
            updateProgress(0)
        } else {
            val result = current * 100L / max
//            if (result > 0.0 && result <= 1.0) {
//                updateProgress(1)
//            } else {
            updateProgress(result.toInt())
//            }
        }
    }

    fun updateProgress(percents: Int) {
        launch {
            progressBar.styleClass.removeAll(STYLES)
            when (percents) {
                0 -> progressBar.styleClass.addAll(EMPTY)
                in 1..99 -> {
                    progressBar.styleClass.addAll(ENABLE)
                    track.prefWidth = maxLength * percents / 100.0
                }
                100 -> progressBar.styleClass.addAll(FULL)
                else -> {
                    progressBar.styleClass.addAll(DISABLE)
                    log.error("Incorrect percents value $percents")
                }
            }
            this@ProgressBar.percents.text = "$percents%"
        }
    }

    companion object {
        private val log by Logger()
        private const val HIDE = "hide"
        private const val DISABLE = "disable"
        private const val ENABLE = "enable"
        private const val EMPTY = "empty"
        private const val FULL = "full"
        private val STYLES = listOf(HIDE, DISABLE, ENABLE, EMPTY, FULL)
    }
}