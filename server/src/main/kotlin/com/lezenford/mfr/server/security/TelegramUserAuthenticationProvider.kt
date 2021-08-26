package com.lezenford.mfr.server.security

import com.lezenford.mfr.server.service.model.TelegramUserService
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component

@Component
class TelegramUserAuthenticationProvider(
    private val telegramUserService: TelegramUserService
) : AuthenticationProvider {
    override fun authenticate(authentication: Authentication): Authentication {
        return telegramUserService.findById(authentication.principal as Long)?.let {
            if (authentication.name != it.username) {
                telegramUserService.save(it.also { it.username = authentication.name })
            }
            TelegramAuthentication(id = it.id, name = it.username, authority = { it.role.name })
        } ?: authentication
    }

    override fun supports(authentication: Class<*>?): Boolean = authentication == TelegramAuthentication::class.java
}