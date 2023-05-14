package com.lezenford.mfr.server.security

import io.jsonwebtoken.JwtParser
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import com.lezenford.mfr.common.SESSION_ID

@Component
class SecurityContextRepository(
    private val jwtParser: JwtParser
) : ServerSecurityContextRepository {
    override fun save(exchange: ServerWebExchange, context: SecurityContext): Mono<Void> = Mono.empty()

    override fun load(exchange: ServerWebExchange): Mono<SecurityContext> = mono {
        exchange.request.cookies[com.lezenford.mfr.common.SESSION_ID]?.firstOrNull()?.value?.parseJwt()
            ?: exchange.request.headers[HttpHeaders.AUTHORIZATION]?.firstOrNull()?.substringAfter(" ")?.parseJwt()
    }

    private fun String.parseJwt(): SecurityContext? {
        return kotlin.runCatching { jwtParser.parse(this) }.onFailure { it.printStackTrace() }.getOrNull()?.let {
            SecurityContextImpl(JwtAuthenticationToken())
        }
    }
}