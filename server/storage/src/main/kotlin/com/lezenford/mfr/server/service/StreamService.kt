package com.lezenford.mfr.server.service

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.server.configuration.History
import com.lezenford.mfr.server.service.model.CategoryService
import com.lezenford.mfr.server.service.model.LauncherService
import com.lezenford.mfr.server.service.model.OverviewService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

@Service
class StreamService(
    private val categoryService: CategoryService,
    private val launcherService: LauncherService,
    private val overviewService: OverviewService,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.IO

    private val buildFlows: ConcurrentHashMap<Int, MutableStateFlow<LocalDateTime>> = ConcurrentHashMap()
    private val launcherFlows: ConcurrentHashMap<SystemType, MutableStateFlow<String>> = ConcurrentHashMap()
    private val connectionFlow: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private val activeConnections: AtomicInteger = AtomicInteger(0)
    val connections: Int
        get() = activeConnections.get()

    fun updateBuildLastUpdateDate(buildId: Int, lastUpdateDate: LocalDateTime) {
        val flow = buildFlows.getOrPut(buildId) { MutableStateFlow(lastUpdateDate) }
        launch { flow.emit(lastUpdateDate) }
    }

    fun updateLauncherVersion(systemType: SystemType, version: String) {
        val flow = launcherFlows.getOrPut(systemType) { MutableStateFlow(version) }
        launch { flow.emit(version) }
    }

    fun connection(clientId: UUID): Flow<Boolean> {
        return connectionFlow.asStateFlow().onStart {
            log.debug("Open rsocket connection from client $clientId")
            CoroutineScope(Dispatchers.History).launch { overviewService.updateClientInfo(clientId.toString()) }
            activeConnections.incrementAndGet()
        }.onCompletion {
            log.debug("Close rsocket connection from client $clientId")
            CoroutineScope(Dispatchers.History).launch { overviewService.updateClientInfo(clientId.toString()) }
            activeConnections.decrementAndGet()
        }
    }

    fun buildFlow(buildId: Int): Flow<LocalDateTime> {
        return buildFlows.getOrPut(buildId) {
            val maxDate = categoryService.findAllByBuildId(buildId).flatMap { it.items }.flatMap { it.files }
                .maxBy { it.lastChangeDate }.lastChangeDate
            MutableStateFlow(maxDate)
        }.asStateFlow()
    }

    fun launcherFlow(systemType: SystemType): Flow<String> {
        return launcherFlows.getOrPut(systemType) {
            val version = launcherService.findBySystem(systemType)?.version ?: "0.0.0"
            MutableStateFlow(version)
        }.asStateFlow()
    }

    companion object {
        private val log by Logger()
    }
}