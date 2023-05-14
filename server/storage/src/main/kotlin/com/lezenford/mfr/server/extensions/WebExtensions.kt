package com.lezenford.mfr.server.extensions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ServerWebExchange
import java.net.URI

fun ServerWebExchange.sendRedirect(path: String) {
    response.statusCode = HttpStatus.TEMPORARY_REDIRECT
    response.headers.location = URI(path)
}