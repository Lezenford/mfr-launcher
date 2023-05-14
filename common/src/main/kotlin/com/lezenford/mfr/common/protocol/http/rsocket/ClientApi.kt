package com.lezenford.mfr.common.protocol.http.rsocket

import kotlinx.coroutines.flow.Flow
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import com.lezenford.mfr.common.extensions.RSocketClientSpec
import com.lezenford.mfr.common.protocol.enums.SystemType
import java.time.LocalDateTime
import java.util.*

interface ClientApi {

    @MessageMapping(connection)
    suspend fun connection(@Payload clientId: UUID): Flow<Boolean>

    @MessageMapping(gameUpdate)
    suspend fun buildLastUpdate(@Payload buildId: Int): Flow<LocalDateTime>

    @MessageMapping(launcherVersion)
    suspend fun launcherVersion(@Payload systemType: SystemType): Flow<String>

    companion object {
        fun connection(clientId: UUID): RSocketClientSpec = RSocketClientSpec(clientId, connection)
        fun buildLastUpdate(buildId: Int): RSocketClientSpec = RSocketClientSpec(buildId, gameUpdate)
        fun launcherVersion(systemType: SystemType): RSocketClientSpec = RSocketClientSpec(systemType, launcherVersion)

        private const val connection = "subscribe/connection"
        private const val gameUpdate = "subscribe/gameUpdate"
        private const val launcherVersion = "subscribe/launcherVersion"
    }
}