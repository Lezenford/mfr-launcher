package com.lezenford.mfr.javafx.component

import javafx.fxml.FXMLLoader
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.javafx.extensions.withFx

abstract class ProgressBar(override val fxmlLoader: FXMLLoader) : FXMLComponent() {
    private val progressBar: VBox by fxml()
    private val track: HBox by fxml()
    private val percents: Label by fxml()
    private val description: Label by fxml()
    protected abstract val maxLength: Int

    suspend fun hide() {
        withFx {
            progressBar.styleClass.removeAll(STYLES)
            progressBar.styleClass.add(HIDE)
        }
    }

    suspend fun disable() {
        withFx {
            progressBar.styleClass.removeAll(STYLES)
            progressBar.styleClass.addAll(DISABLE)
        }
    }

    suspend fun updateDescription(text: String) {
        withFx {
            description.text = text
        }
    }

    suspend fun updateProgress(current: Long, max: Long) {
        if (max == 0L) {
            updateProgress(0)
        } else {
            val result = current * 100L / max
            updateProgress(result.toInt())
        }
    }

    suspend fun updateProgress(percents: Int) {
        withFx {
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
                    throw IllegalArgumentException("Incorrect percents value $percents")
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