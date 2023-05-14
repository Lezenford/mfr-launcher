package com.lezenford.mfr.launcher.service

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.launcher.extension.listener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import java.time.LocalDateTime

object State {
    val gameInstalled: MutableStateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        flow.drop(1).listener { log.info("Set game install value: $it") }
    }
    val gameUpdateStatus: MutableStateFlow<GameStatus> =
        LocalDateTime.now().minusYears(10).let { MutableStateFlow(GameStatus(it, it)) }.also { flow ->
            flow.drop(1).listener { log.info("Set game update status value: $it") }
        }
    val launcherUpdateStatus: MutableStateFlow<LauncherStatus> =
        MutableStateFlow(LauncherStatus("0.0.0", "0.0.0")).also { flow ->
            flow.drop(1).listener { log.info("Set launcher update status value: $it") }
        }
    val currentGameBuild: MutableStateFlow<Int> = MutableStateFlow(0).also { flow ->
        flow.drop(1).listener { log.info("Set current game build value: $it") }
    }
    val onlineMode: MutableStateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        flow.drop(1).listener { log.info("Set online mod value: $it") }
    }
    val serverConnection: MutableStateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        flow.drop(1).listener { log.info("Set server connection status value: $it") }
    }
    val nettyDownloadActive: MutableStateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        flow.drop(1).listener { log.info("Set netty active download status value: $it") }
    }
    val gameVersion: MutableStateFlow<String> = MutableStateFlow("").also { flow ->
        flow.drop(1).listener { log.info("Set game version value: $it") }
    }
    val minimizeToTray: MutableStateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        flow.drop(1).listener { log.info("Set minimize to tray value: $it") }
    }
    val speedLimit: MutableStateFlow<Int> = MutableStateFlow(0).also { flow ->
        flow.drop(1).listener { log.info("Set speed limit value: $it") }
    }

    private val log by Logger()
}

data class LauncherStatus(
    val currentVersion: String,
    val lastVersion: String
) {
    fun needUpdate(): Boolean = currentVersion != lastVersion
}

data class GameStatus(
    val currentUpdateDate: LocalDateTime,
    val serverUpdateDate: LocalDateTime
) {
    fun needUpdate(): Boolean = serverUpdateDate > currentUpdateDate
}