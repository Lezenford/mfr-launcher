package com.lezenford.mfr.server.metric

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import io.netty.handler.traffic.AbstractTrafficShapingHandler
import org.springframework.stereotype.Component

@Component
class TrafficMetric(private val channelTrafficHandler: AbstractTrafficShapingHandler) : MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("netty.traffic.bandwidth.write") {
            channelTrafficHandler.trafficCounter()?.lastWrittenBytes() ?: 0
        }.description("Netty traffic usage")
            .baseUnit("bytes")
            .register(registry)
    }
}