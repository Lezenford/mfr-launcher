package com.lezenford.mfr.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path

@ConstructorBinding
@ConfigurationProperties(prefix = "setting")
data class ServerSettingProperties(
    val buildFolder: String,
    val launcherFolder: String,
    val netty: Netty,
    val git: Git
) {
    data class Netty(
        val port: Int,
        val security: Security?
    ) {
        data class Security(
            val cert: Path,
            val key: Path
        )
    }

    data class Git(
        val url: String,
        val key: String
    )
}