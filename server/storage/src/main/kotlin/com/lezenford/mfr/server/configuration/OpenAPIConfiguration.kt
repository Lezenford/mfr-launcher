package com.lezenford.mfr.server.configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class OpenAPIConfiguration {

    @Bean
    fun openAPI(): OpenAPI = OpenAPI().addServersItem(Server().url("/"))
}