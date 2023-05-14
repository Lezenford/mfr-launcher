package com.lezenford.mfr.manager.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "storage")
data class StorageProperties(
    val serverUrl: String
)