package com.lezenford.mfr.server.metric

import com.lezenford.mfr.server.service.MaintenanceService
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.stereotype.Component

@Component
class MaintenanceMetric(private val maintenanceService: MaintenanceService) : MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("application.maintenance") {
            if (MaintenanceService.Type.values().any { maintenanceService.maintenance(it) }) 1 else 0
        }.description("Maintenance application status")
            .baseUnit("status")
            .register(registry)
    }
}