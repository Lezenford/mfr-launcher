package com.lezenford.mfr.server

import com.lezenford.mfr.server.configuration.properties.ApplicationProperties
import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(value = [ServerSettingProperties::class, ApplicationProperties::class])
class StorageServerApplication

fun main(args: Array<String>) {
    runApplication<StorageServerApplication>(*args)
}