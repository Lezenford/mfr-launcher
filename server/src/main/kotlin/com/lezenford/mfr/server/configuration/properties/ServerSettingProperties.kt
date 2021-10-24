package com.lezenford.mfr.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path

@ConstructorBinding
@ConfigurationProperties(prefix = "setting")
data class ServerSettingProperties(
    val launcherFolder: String,
    val netty: Netty,
    val build: Git,
    val manual: Git
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
        val local: String,
        val remote: String,
        val key: String
    )
}