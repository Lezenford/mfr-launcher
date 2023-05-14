package com.lezenford.mfr.manager.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "telegram")
data class TelegramProperties(
    val username: String,
    val token: String,
    val path: String,
    val admin: Long,
    val registerWebhook: Boolean = true,
    val registerCommands: Boolean = true,
    val serverAddress: String = "api.telegram.org"
)