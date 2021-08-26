package com.lezenford.mfr.server.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import ru.fullrest.mfr.common.exception.ServerMaintenanceException

@ControllerAdvice
class ControllerExceptionHandler {

    @ExceptionHandler(ServerMaintenanceException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun catchServerMaintenanceException(e: ServerMaintenanceException) {
    }
}