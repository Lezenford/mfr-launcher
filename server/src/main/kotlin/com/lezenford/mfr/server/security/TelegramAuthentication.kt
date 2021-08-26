package com.lezenford.mfr.server.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority

class TelegramAuthentication(
    private val id: Long,
    private val name: String?,
    private val authority: GrantedAuthority = GrantedAuthority { GUEST }
) : Authentication {
    override fun getPrincipal(): Long = id
    override fun getName(): String? = name
    override fun getAuthorities() = setOf(authority)
    override fun getCredentials(): Any? = null
    override fun getDetails(): Any? = null
    override fun isAuthenticated(): Boolean = true
    override fun setAuthenticated(isAuthenticated: Boolean) {}

    companion object {
        private const val GUEST = "GUEST"
    }
}