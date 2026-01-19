package com.lezenford.mfr.launcher.service.initiator

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.protocol.file.SCHEMA_FILE_NAME
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.config.properties.GameProperties
import com.lezenford.mfr.launcher.exception.handler.AbstractExceptionHandler
import com.lezenford.mfr.launcher.extension.ModifyFiles
import com.lezenford.mfr.launcher.extension.listener
import com.lezenford.mfr.launcher.model.entity.Properties
import com.lezenford.mfr.launcher.service.GameStatus
import com.lezenford.mfr.launcher.service.LauncherStatus
import com.lezenford.mfr.launcher.service.Location
import com.lezenford.mfr.launcher.service.OpenMwService
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.model.PropertiesService
import com.lezenford.mfr.launcher.service.provider.KtorProvider
import com.lezenford.mfr.schema.v1.Schema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

abstract class InitApplicationInitiator : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    protected abstract val exceptionHandler: AbstractExceptionHandler
    protected abstract val applicationProperties: ApplicationProperties
    protected abstract val gameProperties: GameProperties
    protected abstract val propertyService: PropertiesService
    protected abstract val openMwService: OpenMwService
    protected abstract val ktorProvider: KtorProvider

    val init: Job by lazy {
        launch {
            Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)

            log.info("Try to find online mod value")
            propertyService.findByKey(Properties.Key.ONLINE_MODE)?.value?.toBoolean()?.also {
                State.onlineMode.emit(it)
            } ?: propertyService.save(Properties(Properties.Key.ONLINE_MODE, true.toString())).also {
                State.onlineMode.emit(true)
            }

            log.info("Try to find minimize to tray value")
            propertyService.findByKey(Properties.Key.MINIMIZE_TO_TRAY)?.value?.toBoolean()?.also {
                State.minimizeToTray.emit(it)
            } ?: propertyService.save(Properties(Properties.Key.MINIMIZE_TO_TRAY, false.toString())).also {
                State.minimizeToTray.emit(false)
            }

            log.info("Try to find location value")
            propertyService.findByKey(Properties.Key.LOCATION)?.value?.also {
                State.location.emit(Location.valueOf(it))
            }

            prepareListeners()

            propertyService.findByKey(Properties.Key.NEW_RELEASE_INSTALLED)?.also {
                State.gameInstalled.emit(true)
            }

            complete()
        }
    }

    private suspend fun prepareListeners() {
        State.gameInstalled.listener { installed ->
            if (installed) {
                ModifyFiles.esmFileList.forEach { (fileName, modifiedDate) ->
                    applicationProperties.gameFolder.resolve(ModifyFiles.fileDirectory).resolve(fileName)
                        .takeIf { it.exists() }
                        ?.also { it.toFile().setLastModified(modifiedDate) }
                }

                applicationProperties.gameFolder.resolve(SCHEMA_FILE_NAME).takeIf { it.exists() }?.also {
                    val schema = Schema.parseFrom(it.readBytes())
                    State.schema.emit(schema)
                    State.gameVersion.emit(schema.version)
                }

                openMwService.prepareTemplates()
            }
        }

        State.gameVersion.listener { version ->
            if (version.isNotBlank()) {
                ModifyFiles.esmFileList.forEach { (fileName, modifiedDate) ->
                    applicationProperties.gameFolder.resolve(ModifyFiles.fileDirectory).resolve(fileName)
                        .takeIf { it.exists() }
                        ?.also { it.toFile().setLastModified(modifiedDate) }
                }
            }
        }

        State.onlineMode.listener { online ->
            if (online) {
                streamUpdateSubscribe(5.minutes) {
                    coroutineScope {
                        val euResult = async {
                            ktorProvider.checkAvailable("https://${applicationProperties.server.euLocation.address}")?.also {
                                State.serverConnection.emit(true)
                            }
                        }
                        val ruResult = async {
                            ktorProvider.checkAvailable("https://${applicationProperties.server.ruLocation.address}")?.also {
                                State.serverConnection.emit(true)
                            }
                        }
                        if (euResult.await() == null && ruResult.await() == null) {
                            State.serverConnection.emit(false)
                        }
                    }
                }

                streamUpdateSubscribe {
                    coroutineScope {
                        State.serverConnection.first { it }
                        val euResult = async {
                            ktorProvider.checkAvailable("https://${applicationProperties.server.euLocation.address}")?.also {
                                State.serverConnection.emit(true)
                            }
                        }
                        val ruResult = async {
                            ktorProvider.checkAvailable("https://${applicationProperties.server.ruLocation.address}")?.also {
                                State.serverConnection.emit(true)
                            }
                        }
                        val ru = ruResult.await()
                        val eu = euResult.await()
                        when {
                            eu == null && ru != null -> State.location.emit(Location.EU)
                            eu != null && ru == null -> State.location.emit(Location.RU)
                        }
                        State.availableEuLocation.emit(eu != null)
                        State.availableRuLocation.emit(ru != null)
                    }
                }

                streamUpdateSubscribe {
                    State.serverConnection.first { it }
                    val version = State.schema.value?.version
                    if (State.gameInstalled.value && version != null) {
                        State.gameUpdateStatus.emit(
                            GameStatus(
                                currentVersion = version,
                                serverVersion = ktorProvider.findActiveGameVersion()
                            )
                        )
                    }
                }

                streamUpdateSubscribe {
                    State.serverConnection.first { it }
                    val version = ktorProvider.findActiveLauncherVersion()
                    State.launcherUpdateStatus.emit(
                        LauncherStatus(
                            currentVersion = applicationProperties.version,
                            lastVersion = version
                        )
                    )
                }
            }
        }

        State.minimizeToTray.listener {
            propertyService.updateValue(Properties.Key.MINIMIZE_TO_TRAY, it.toString())
        }

        State.location.listener {
            propertyService.updateValue(Properties.Key.LOCATION, it.toString())
        }
    }

    private fun streamUpdateSubscribe(delay: Duration = 1.hours, action: suspend () -> Unit) {
        launch {
            while (State.onlineMode.value) {
                runCatching {
                    action()
                }.onFailure {
                    log.error("Connection error. ${it.message}")
                }
                delay(delay)
            }
        }.also { connectionJob ->
            launch { State.onlineMode.listener { if (it.not()) connectionJob.cancel() } }
        }
    }

    protected abstract suspend fun complete()

    companion object {
        private val log by Logger()
    }
}