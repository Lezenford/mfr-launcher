package com.lezenford.mfr.server.annotation

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.protocol.netty.ServerMaintenanceMessage
import com.lezenford.mfr.server.service.MaintenanceService
import io.netty.channel.group.ChannelGroup
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component

@Aspect
@Component
class MaintenanceProcessor(
    private val clients: ChannelGroup,
    private val gameDownloadClients: ChannelGroup,
    private val launcherDownloadClients: ChannelGroup,
    private val maintenanceService: MaintenanceService
) {
    @Before("@annotation(available)")
    fun beforeReady(available: Available) {
        if (available.type.any { maintenanceService.maintenance(it) }) {
            throw com.lezenford.mfr.common.exception.ServerMaintenanceException("The server is in process of maintenance")
        }
    }

    @Around("@annotation(maintenance)", argNames = "maintenance")
    fun aroundMaintenance(pjp: ProceedingJoinPoint, maintenance: Maintenance): Any? {
        return if (maintenanceService.setUp(maintenance.type).not()) {
            try {
                log.info("Server start maintenance mod ${maintenance.type}")
                when (maintenance.type) {
                    MaintenanceService.Type.GAME -> gameDownloadClients
                    MaintenanceService.Type.LAUNCHER -> launcherDownloadClients
                    MaintenanceService.Type.ALL -> clients
                    else -> null
                }?.map { channel ->
                    channel.writeAndFlush(ServerMaintenanceMessage()).addListener { channel.close() }
                    channel.closeFuture()
                }?.forEach { it.sync() }?.also {
                    log.info("All client connections are finished")
                }
                pjp.proceed()
            } finally {
                maintenanceService.setDown(maintenance.type)
                log.info("Server has finished maintenance mod ${maintenance.type}")
            }
        } else {
            throw com.lezenford.mfr.common.exception.ServerMaintenanceException("Server already has maintenance for operation type ${maintenance.type}")
        }
    }

    @AfterThrowing(value = "@annotation(com.lezenford.mfr.server.annotation.Maintenance)", throwing = "e")
    fun exceptionMaintenanceHandle(e: Throwable) {
        if (e is com.lezenford.mfr.common.exception.ServerMaintenanceException) {
            log.info(e.message)
        } else {
            log.error("Maintenance throw an exception", e)
        }
    }

    companion object {
        private val log by Logger()
    }
}