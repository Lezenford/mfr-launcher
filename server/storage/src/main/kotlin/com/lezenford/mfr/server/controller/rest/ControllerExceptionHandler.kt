package com.lezenford.mfr.server.controller.rest

import com.lezenford.mfr.common.extensions.Logger
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus


@ControllerAdvice
class ControllerExceptionHandler {

    @ExceptionHandler(com.lezenford.mfr.common.exception.ServerMaintenanceException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun catchServerMaintenanceException(e: com.lezenford.mfr.common.exception.ServerMaintenanceException) {
        log.info("Request broken. Server in maintenance")
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun catchException(e: Exception) {
        log.error("Rest request error", e)
    }

    companion object {
        private val log by Logger()
    }
}