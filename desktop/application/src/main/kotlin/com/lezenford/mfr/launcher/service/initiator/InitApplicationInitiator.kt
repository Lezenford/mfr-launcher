package com.lezenford.mfr.launcher.service.initiator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.config.properties.GameProperties
import com.lezenford.mfr.launcher.exception.handler.AbstractExceptionHandler
import com.lezenford.mfr.launcher.extension.ModifyFiles
import com.lezenford.mfr.launcher.extension.listener
import com.lezenford.mfr.launcher.model.entity.Properties
import com.lezenford.mfr.launcher.service.GameStatus
import com.lezenford.mfr.launcher.service.LauncherStatus
import com.lezenford.mfr.launcher.service.OpenMwService
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.model.PropertiesService
import com.lezenford.mfr.launcher.service.provider.RSocketProvider
import com.lezenford.mfr.launcher.service.provider.RestProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.exists

abstract class InitApplicationInitiator : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    protected abstract val exceptionHandler: AbstractExceptionHandler
    protected abstract val applicationProperties: ApplicationProperties
    protected abstract val gameProperties: GameProperties
    protected abstract val propertyService: PropertiesService
    protected abstract val restProvider: RestProvider
    protected abstract val openMwService: OpenMwService
    protected abstract val rSocketProvider: RSocketProvider
    protected abstract val objectMapper: ObjectMapper

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

            log.info("Try to find speed limit value")
            propertyService.findByKey(Properties.Key.SPEED_LIMIT)?.value?.toInt()?.also {
                State.speedLimit.emit(it)
            } ?: propertyService.save(Properties(Properties.Key.SPEED_LIMIT, 0.toString())).also {
                State.speedLimit.emit(0)
            }

            prepareListeners()

            propertyService.findByKey(Properties.Key.GAME_INSTALLED)?.also {
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

                State.gameVersion.emit(gameProperties.version)

                openMwService.prepareTemplates()
            }
        }

        State.onlineMode.listener { online ->
            if (online) {
                val buildsDto = restProvider.findAllBuild()

                val build = propertyService.findByKey(Properties.Key.SELECTED_BUILD) ?: buildsDto.first().run {
                    Properties(key = Properties.Key.SELECTED_BUILD, value = name).also { propertyService.save(it) }
                }

                log.info("Try to find build id for name: ${build.value}")
                State.currentGameBuild.emit(buildsDto.first { it.name == build.value }.id)

                streamUpdateSubscribe {
                    rSocketProvider.connection(applicationProperties.clientId).onStart {
                        State.serverConnection.emit(true)
                    }.onCompletion {
                        State.serverConnection.emit(false)
                        it?.also { throw it }
                    }.collect()
                }

                streamUpdateSubscribe {
                    State.serverConnection.first { it }
                    rSocketProvider.buildLastUpdate(State.currentGameBuild.value).collect { serverUpdateDate ->
                        log.info("Receive game last update date: $serverUpdateDate")
                        propertyService.findByKey(Properties.Key.LAST_UPDATE_DATE)?.value?.also {
                            State.gameUpdateStatus.emit(
                                GameStatus(
                                    currentUpdateDate = objectMapper.readValue(it),
                                    serverUpdateDate = serverUpdateDate
                                )
                            )
                        }
                    }
                }

                streamUpdateSubscribe {
                    State.serverConnection.first { it }
                    rSocketProvider.launcherVersion(applicationProperties.platform).collect { launcherLastVersion ->
                        log.info("Receive launcher last version: $launcherLastVersion")
                        State.launcherUpdateStatus.emit(
                            LauncherStatus(
                                currentVersion = applicationProperties.version,
                                lastVersion = launcherLastVersion
                            )
                        )
                    }
                }
            }
        }

        State.minimizeToTray.listener {
            propertyService.updateValue(Properties.Key.MINIMIZE_TO_TRAY, it.toString())
        }

        State.speedLimit.listener {
            propertyService.updateValue(Properties.Key.SPEED_LIMIT, it.toString())
        }
    }

    private fun streamUpdateSubscribe(action: suspend () -> Unit) {
        launch {
            while (State.onlineMode.value) {
                runCatching {
                    action()
                }.onFailure {
                    log.error("Connection error. ${it.message}")
                    delay(1000)
                }
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