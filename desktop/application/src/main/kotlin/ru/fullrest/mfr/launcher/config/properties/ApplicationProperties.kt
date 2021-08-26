package ru.fullrest.mfr.launcher.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import ru.fullrest.mfr.common.api.SystemType
import java.nio.file.Path
import java.util.*

@ConstructorBinding
@ConfigurationProperties(prefix = "application")
data class ApplicationProperties(
    val gameFolder: Path,
    val version: String,
    val platform: SystemType,
    val server: Server,
    val social: Social,
    val clientId: UUID,
    val readme: Path
) {
    data class Server(
        val address: String,
        val tcpPort: Int
    )

    data class Social(
        val forum: String,
        val discord: String,
        val youtube: String,
        val vk: String,
        val patreon: String
    )
}