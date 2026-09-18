package com.lezenford.mfr.launcher.service

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.launcher.extension.listener
import com.lezenford.mfr.schema.v1.Schema
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import java.time.LocalDateTime

object State {
    val gameInstalled: MutableStateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        flow.drop(1).listener { log.info("Set game install value: $it") }
    }
    val gameUpdateStatus: MutableStateFlow<GameStatus> = MutableStateFlow(GameStatus("0", "0")).also { flow ->
            flow.drop(1).listener { log.info("Set game update status value: $it") }
        }
    val launcherUpdateStatus: MutableStateFlow<LauncherStatus> =
        MutableStateFlow(LauncherStatus("0.0.0", "0.0.0")).also { flow ->
            flow.drop(1).listener { log.info("Set launcher update status value: $it") }
        }
    val onlineMode: MutableStateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        flow.drop(1).listener { log.info("Set online mod value: $it") }
    }
    val serverConnection: MutableStateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        flow.drop(1).listener { log.info("Set server connection status value: $it") }
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
    val location: MutableStateFlow<Location> = MutableStateFlow(Location.RU).also { flow ->
        flow.listener { log.info("Set location value: $it") }
    }
    val availableRuLocation: MutableStateFlow<Boolean> = MutableStateFlow(true).also { flow ->
        flow.listener { log.info("Set available ru location value: $it") }
    }
    val availableEuLocation: MutableStateFlow<Boolean> = MutableStateFlow(true).also { flow ->
        flow.listener { log.info("Set available eu location value: $it") }
    }
    val schema: MutableStateFlow<Schema?> = MutableStateFlow<Schema?>(null).also { flow ->
        flow.listener { log.info("Set schema value with version: ${it?.version}") }
    }
    val clientId: MutableStateFlow<String?> = MutableStateFlow<String?>(null).also { flow ->
        flow.listener { log.info("Set cleintId value: $it") }
    }
    val selectedBuild: MutableStateFlow<String?> = MutableStateFlow<String?>(null).also { flow ->
        flow.listener { log.info("Set selected build value: $it") }
    }
    val availableBuilds: MutableStateFlow<List<String>> = MutableStateFlow(emptyList<String>()).also { flow ->
        flow.listener { log.info("Set available builds value: $it") }
    }
    val newLineAvailable: MutableStateFlow<String?> = MutableStateFlow<String?>(null).also { flow ->
        flow.listener { log.info("Set new line available value: $it") }
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
    val currentVersion: String,
    val serverVersion: String
) {
    fun needUpdate(): Boolean = serverVersion != currentVersion
}

enum class Location {
    RU, EU
}