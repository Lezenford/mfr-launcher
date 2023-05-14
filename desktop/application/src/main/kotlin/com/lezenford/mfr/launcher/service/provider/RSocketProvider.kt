package com.lezenford.mfr.launcher.service.provider

import com.lezenford.mfr.common.extensions.spec
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.http.rsocket.ClientApi
import kotlinx.coroutines.flow.Flow
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveFlow
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.*

@Component
class RSocketProvider(
    private val rSocketClient: RSocketRequester
) : ClientApi {
    override suspend fun connection(clientId: UUID): Flow<Boolean> =
        rSocketClient.spec(ClientApi.connection(clientId)).retrieveFlow()

    override suspend fun buildLastUpdate(buildId: Int): Flow<LocalDateTime> =
        rSocketClient.spec(ClientApi.buildLastUpdate(buildId)).retrieveFlow()

    override suspend fun launcherVersion(systemType: SystemType): Flow<String> =
        rSocketClient.spec(ClientApi.launcherVersion(systemType)).retrieveFlow()
}