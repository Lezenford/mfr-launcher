package com.lezenford.mfr.server.security

import org.springframework.security.authentication.AbstractAuthenticationToken

class JwtAuthenticationToken : AbstractAuthenticationToken(emptyList()) {
    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any = RESULT

    override fun getPrincipal(): Any = RESULT

    companion object {
        private val RESULT = Any()
    }
}