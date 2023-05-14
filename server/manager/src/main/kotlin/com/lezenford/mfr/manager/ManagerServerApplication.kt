package com.lezenford.mfr.manager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ManagerServerApplication

fun main(args: Array<String>) {
    runApplication<ManagerServerApplication>(*args)
}