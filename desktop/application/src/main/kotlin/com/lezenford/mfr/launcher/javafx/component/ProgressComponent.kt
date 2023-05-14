package com.lezenford.mfr.launcher.javafx.component

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.launcher.extension.bind
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.factory.TaskFactory
import com.lezenford.mfr.launcher.service.provider.NettyProvider
import com.lezenford.mfr.launcher.task.Task
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.control.Button
import javafx.scene.layout.VBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ProgressComponent(
    override val fxmlLoader: FXMLLoader,
    private val nettyProvider: NettyProvider,
    private val factory: TaskFactory,
) : SimpleLauncherProgressBar(fxmlLoader) {
    private val updateButton: Button by fxml()
    private val updateLogo: VBox by fxml()

    init {
        launch { setStatus(Status.DISABLE) }
    }

    fun lock(status: Boolean) {
        if (status) {
            BLOCK_MUTEX.tryLock()
        } else {
            BLOCK_MUTEX.unlock()
        }
    }

    suspend fun setStatus(status: Status) {
        BLOCK_MUTEX.withLock {
            withContext(Dispatchers.JavaFx) {
                status.applyStyle.invoke(this@ProgressComponent)
                updateButton.text = status.text
                updateButton.onAction = EventHandler {
                    CoroutineScope(Dispatchers.IO).launch { status.action.invoke(this@ProgressComponent) }
                }
            }
        }
    }

    suspend fun executeTask(task: Task<Unit, Unit>) {
        try {
            withContext(Dispatchers.IO) {
                setStatus(Status.BLOCK)
                BLOCK_MUTEX.lock()
                bind(task) {
                    launch {
                        State.nettyDownloadActive.collect {
                            if (it) {
                                setStatus(Status.PAUSE)
                            } else {
                                setStatus(Status.BLOCK)
                            }
                        }
                    }.apply {
                        it.execute(Unit)
                    }.cancel()
                }

                hide()
            }
        } catch (e: Exception) {
            disable()
            updateDescription("Произошла ошибка")
            throw e
        } finally {
            BLOCK_MUTEX.unlock()
            setStatus(Status.DISABLE)
        }
    }

    companion object {
        private val log by Logger()
        private const val BUTTON_LAVA = "lava"
        private const val BUTTON_LOCKED = "locked"
        private const val LOGO_UPDATE_READY = "update"
        private val styles = listOf(BUTTON_LAVA, BUTTON_LOCKED)
        private val BLOCK_MUTEX = Mutex()
    }

    enum class Status(val text: String) {
        DISABLE("Обновлений нет") {
            override val applyStyle: suspend ProgressComponent.() -> Unit = {
                updateButton.styleClass.removeAll(styles)
                updateButton.styleClass.add(BUTTON_LOCKED)
                updateLogo.styleClass.remove(LOGO_UPDATE_READY)
            }
        },
        BLOCK("Подождите...") {
            override val applyStyle: suspend ProgressComponent.() -> Unit = {
                updateButton.styleClass.removeAll(styles)
                updateButton.styleClass.add(BUTTON_LOCKED)
                updateLogo.styleClass.remove(LOGO_UPDATE_READY)
            }
        },
        PAUSE("Приостановить") {
            override val applyStyle: suspend ProgressComponent.() -> Unit = {
                updateButton.styleClass.removeAll(styles)
                updateLogo.styleClass.remove(LOGO_UPDATE_READY)
            }
            override val action: suspend ProgressComponent.() -> Unit = {
                setStatus(RESUME)
                nettyProvider.pause()
            }
        },
        RESUME("Продолжить") {
            override val applyStyle: suspend ProgressComponent.() -> Unit = {
                updateButton.styleClass.removeAll(styles)
                updateLogo.styleClass.remove(LOGO_UPDATE_READY)
            }
            override val action: suspend ProgressComponent.() -> Unit = {
                setStatus(PAUSE)
                nettyProvider.resume()
            }
        },
        GAME_UPDATE("Обновить игру") {
            override val applyStyle: suspend ProgressComponent.() -> Unit = {
                updateButton.styleClass.removeAll(styles)
                updateButton.styleClass.add(BUTTON_LAVA)
                updateLogo.styleClass.add(LOGO_UPDATE_READY)
            }
            override val action: suspend ProgressComponent.() -> Unit = { executeTask(factory.gameUpdateTask()) }
        },
        GAME_INSTALL("Установить игру") {
            override val applyStyle: suspend ProgressComponent.() -> Unit = {
                updateButton.styleClass.removeAll(styles)
                updateButton.styleClass.add(BUTTON_LAVA)
                updateLogo.styleClass.remove(LOGO_UPDATE_READY)
            }
            override val action: suspend ProgressComponent.() -> Unit = { executeTask(factory.gameInstallTask()) }
        },
        LAUNCHER_UPDATE("Обновить лаунчер") {
            override val applyStyle: suspend ProgressComponent.() -> Unit = {
                updateButton.styleClass.removeAll(styles)
                updateButton.styleClass.add(BUTTON_LAVA)
                updateLogo.styleClass.remove(LOGO_UPDATE_READY)
            }
            override val action: suspend ProgressComponent.() -> Unit = { executeTask(factory.launcherUpdateTask()) }
        };

        abstract val applyStyle: suspend ProgressComponent.() -> Unit
        open val action: suspend ProgressComponent.() -> Unit = {}
    }
}