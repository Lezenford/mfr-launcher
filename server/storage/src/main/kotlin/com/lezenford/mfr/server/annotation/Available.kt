package com.lezenford.mfr.server.annotation

import com.lezenford.mfr.server.service.MaintenanceService

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class Available(
    vararg val type: MaintenanceService.Type
)
