package ru.fullrest.mfr.launcher.javafx.component

import javafx.event.EventHandler
import javafx.scene.control.Button

abstract class DownloadButton(
    private val downloadEvent: DownloadButton.() -> Unit
) : Button() {

    fun setStatus(status: Status) {
        onMouseClicked = when (status) {
            Status.DOWNLOAD -> {
                styleClass.removeAll(REMOVE_STYLE, DOWNLOAD_STYLE)
                styleClass.add(DOWNLOAD_STYLE)
                EventHandler { downloadEvent() }
            }
            Status.REMOVE -> {
                styleClass.removeAll(REMOVE_STYLE, DOWNLOAD_STYLE)
                styleClass.add(REMOVE_STYLE)
                EventHandler { removeOption() }
            }
        }
    }

    protected abstract fun removeOption()

    enum class Status {
        DOWNLOAD, REMOVE
    }

    companion object {
        private const val REMOVE_STYLE = "remove"
        private const val DOWNLOAD_STYLE = "download"
    }
}