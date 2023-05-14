package com.lezenford.mfr.server.configuration

import com.lezenford.mfr.server.configuration.properties.ServerSettingProperties
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JwtConfiguration(
    private val properties: ServerSettingProperties
) {
    @Bean
    fun jwtParser(): JwtParser = Jwts.parserBuilder().setSigningKey(properties.web.security.key).build()
}