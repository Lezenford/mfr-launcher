package ru.fullrest.mfr.launcher.javafx.component

import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.control.Button
import javafx.scene.layout.VBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import ru.fullrest.mfr.common.extensions.Logger
import ru.fullrest.mfr.javafx.component.FXMLComponent
import ru.fullrest.mfr.launcher.javafx.TaskFactory
import java.util.concurrent.atomic.AtomicReference

class UpdateComponent(
    override val fxmlLoader: FXMLLoader,
    private val taskFactory: TaskFactory
) : FXMLComponent(), CoroutineScope {
    override val coroutineContext = Dispatchers.JavaFx

    val progressBar = LauncherProgressBar(fxmlLoader)
    private val updateButton: Button by fxml()
    private val updateLogo: VBox by fxml()
    private val task: AtomicReference<Task> = AtomicReference()
    private val currentStatus = AtomicReference<Status>()

    fun setStatus(status: Status) {
        launch {
            when (status) {
                Status.DISABLE -> {
                    updateButton.text = "Обновлений нет"
                    updateButton.styleClass.removeAll(styles)
                    updateButton.styleClass.add(BUTTON_LOCKED)
                    updateLogo.styleClass.remove(LOGO_UPDATE_READY)
                    updateButton.onMouseClicked = EventHandler {}
                }
                Status.UPDATE_READY -> {
                    if (currentStatus.get() != Status.LAUNCHER_UPDATE_READY) {
                        updateButton.text = "Обновить"
                        updateButton.styleClass.removeAll(styles)
                        updateButton.styleClass.add(BUTTON_LAVA)
                        updateLogo.styleClass.remove(LOGO_UPDATE_READY)
                        updateLogo.styleClass.add(LOGO_UPDATE_READY)
                        updateButton.onMouseClicked = EventHandler { updateGame() }
                    }
                }
                Status.LAUNCHER_UPDATE_READY -> {
                    updateButton.text = "Обновить Launcher"
                    updateButton.styleClass.removeAll(styles)
                    updateButton.styleClass.add(BUTTON_LAVA)
                    updateLogo.styleClass.remove(LOGO_UPDATE_READY)
                    updateButton.onMouseClicked = EventHandler { updateLauncher() }
                }
                Status.INSTALL_READY -> {
                    if (currentStatus.get() != Status.LAUNCHER_UPDATE_READY) {
                        updateButton.text = "Установить игру"
                        updateButton.styleClass.removeAll(styles)
                        updateButton.styleClass.add(BUTTON_LAVA)
                        updateLogo.styleClass.remove(LOGO_UPDATE_READY)
                        updateButton.onMouseClicked = EventHandler { installGame() }
                    }
                }
                Status.CANCEL -> {
                    updateButton.text = "Остановить"
                    updateButton.styleClass.removeAll(styles)
                    updateLogo.styleClass.remove(LOGO_UPDATE_READY)
                    updateButton.onMouseClicked = EventHandler {
                        task.get()?.invoke
                    }
                }
            }
            currentStatus.set(status)
        }
    }

    private fun installGame() {
        launch(Dispatchers.Default) {
            kotlin.runCatching {
                taskFactory.gameInstallTask().execute(progressBar)
            }.onSuccess {
                setStatus(Status.DISABLE)
            }.onFailure {
                setStatus(Status.INSTALL_READY)
            }.getOrThrow()
        }.also {
            task.set(
                Task {
                    it.cancel()
                    setStatus(Status.INSTALL_READY)
                }
            )
        }
        setStatus(Status.CANCEL)
    }

    private fun updateGame() {
        launch(Dispatchers.Default) {
            taskFactory.gameUpdateTask().execute(progressBar)
            setStatus(Status.DISABLE)
        }.also {
            task.set(
                Task {
                    it.cancel()
                    setStatus(Status.INSTALL_READY)
                }
            )
        }
        setStatus(Status.CANCEL)
    }

    private fun updateLauncher() {
        launch(Dispatchers.Default) {
            taskFactory.launcherUpdateTask().execute(progressBar)
            setStatus(Status.LAUNCHER_UPDATE_READY)
        }.also {
            task.set(
                Task {
                    it.cancel()
                    setStatus(Status.LAUNCHER_UPDATE_READY)
                }
            )
        }
        setStatus(Status.CANCEL)
    }

    companion object {
        private val log by Logger()
        private const val BUTTON_LAVA = "lava"
        private const val BUTTON_LOCKED = "locked"
        private const val LOGO_UPDATE_READY = "update"
        private val styles = listOf(BUTTON_LAVA, BUTTON_LOCKED)
    }

    enum class Status {
        DISABLE, UPDATE_READY, LAUNCHER_UPDATE_READY, INSTALL_READY, CANCEL
    }

    private inner class Task(
        private val init: () -> Unit
    ) {
        val invoke by lazy { init() }
    }
}