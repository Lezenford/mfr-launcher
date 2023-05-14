package com.lezenford.mfr.server.controller.rest

import com.lezenford.mfr.server.annotation.Available
import com.lezenford.mfr.server.extensions.sendRedirect
import com.lezenford.mfr.server.service.MaintenanceService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RestController
class ManualController {

    @GetMapping("/readme", "/")
    @Available(MaintenanceService.Type.MANUAL)
    fun manual(exchange: ServerWebExchange) = when (exchange.request.path.value()) {
        "/readme", "/" -> exchange.sendRedirect("readme/index.html")
        "/readme/" -> exchange.sendRedirect("index.html")
        else -> exchange.response.statusCode = HttpStatus.NOT_FOUND
    }
}