package com.lezenford.mfr.server

import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import com.lezenford.mfr.server.configuration.properties.TelegramProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(value = [ServerSettingProperties::class, TelegramProperties::class])
class ServerApplication

fun main(args: Array<String>) {
    runApplication<ServerApplication>(*args)
}