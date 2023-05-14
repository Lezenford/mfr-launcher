package com.lezenford.mfr.server.metric

import com.lezenford.mfr.server.netty.CLIENT_ID
import com.lezenford.mfr.server.service.StreamService
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import io.netty.channel.group.ChannelGroup
import org.springframework.stereotype.Component

@Component
class ClientMetric(
    private val clients: ChannelGroup,
    private val streamService: StreamService
) : MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder("netty.user.connected") {
            clients.filter { it.isActive }.mapNotNull { it.attr(CLIENT_ID).get() }.distinct().count()
        }.description("Netty client active connections")
            .baseUnit("count")
            .register(registry)
        Gauge.builder("netty.connection.active") {
            clients.count { it.isActive }
        }.description("Netty total active connections")
            .baseUnit("count")
            .register(registry)
        Gauge.builder("rsocket.user.active") {
            streamService.connections
        }.description("RSocket user active connections")
            .baseUnit("count")
            .register(registry)
    }
}