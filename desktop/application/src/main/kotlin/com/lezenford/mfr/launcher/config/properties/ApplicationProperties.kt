package com.lezenford.mfr.launcher.config.properties

import com.lezenford.mfr.common.protocol.enums.SystemType
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
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
    val readme: Readme
) {

    data class Readme(
        val local: Path,
        val remote: String
    )

    data class Server(
        val http: Address,
        val tcp: Address,
        val connectionCount: Int
    )

    data class Address(
        val dnsName: String,
        val ip: String,
        val port: Int
    )

    data class Social(
        val forum: String,
        val discord: String,
        val youtube: String,
        val vk: String,
        val patreon: String
    )
}