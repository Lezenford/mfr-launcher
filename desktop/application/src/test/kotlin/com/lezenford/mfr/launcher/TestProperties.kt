package com.lezenford.mfr.launcher

import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import java.nio.file.Path
import java.util.UUID

fun testProperties(gameFolder: Path, connectionCount: Int = 2) = ApplicationProperties(
    gameFolder = gameFolder,
    version = "0.0.0",
    platform = SystemType.WINDOWS,
    server = ApplicationProperties.Server(
        euLocation = ApplicationProperties.Server.Location("eu.example"),
        ruLocation = ApplicationProperties.Server.Location("ru.example"),
        connectionCount = connectionCount
    ),
    social = ApplicationProperties.Social("", "", "", "", "", ""),
    clientId = UUID.randomUUID(),
    readme = ApplicationProperties.Readme(gameFolder.resolve("readme"), "")
)
