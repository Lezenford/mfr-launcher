package com.lezenford.mfr.server.controller.rest

import com.lezenford.mfr.server.extensions.sendRedirect
import io.jsonwebtoken.JwtParser
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import com.lezenford.mfr.common.SESSION_ID
import java.time.Duration

@RestController
class SwaggerAuthController(
    private val jwtParser: JwtParser
) {

    @GetMapping("/swagger")
    suspend fun openSwagger(exchange: ServerWebExchange, @RequestParam key: String) {
        kotlin.runCatching {
            jwtParser.parse(key)
        }.onSuccess {
            exchange.response.addCookie(
                ResponseCookie.from(com.lezenford.mfr.common.SESSION_ID, key)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .maxAge(Duration.ofHours(1))
                    .build()
            )
            exchange.sendRedirect("/webjars/swagger-ui/index.html")
        }.onFailure {
            exchange.response.statusCode = HttpStatus.NOT_FOUND
        }
    }
}