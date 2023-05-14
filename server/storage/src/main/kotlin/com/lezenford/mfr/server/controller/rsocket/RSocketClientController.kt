package com.lezenford.mfr.server.controller.rsocket

import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.http.rsocket.ClientApi
import com.lezenford.mfr.server.service.StreamService
import kotlinx.coroutines.flow.Flow
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException
import org.springframework.stereotype.Controller
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.*

@Controller
@MessageMapping
class RSocketClientController(
    private val streamService: StreamService
) : ClientApi {

    override suspend fun connection(clientId: UUID): Flow<Boolean> = streamService.connection(clientId)

    override suspend fun buildLastUpdate(buildId: Int): Flow<LocalDateTime> = streamService.buildFlow(buildId)

    override suspend fun launcherVersion(systemType: SystemType): Flow<String> =
        streamService.launcherFlow(systemType)

    @MessageExceptionHandler(MethodArgumentResolutionException::class)
    fun handleRSocketMethodArgumentResolutionException(e: MethodArgumentResolutionException): Mono<ResponseEntity<Unit>> {
        return Mono.just(ResponseEntity.badRequest().build())
    }

    @MessageExceptionHandler(Exception::class)
    fun handleRSocketException(e: Exception): Mono<ResponseEntity<Unit>> {
        log.error("RSocket handle ad error: ${e.message}", e)
        return Mono.just(ResponseEntity.internalServerError().build())
    }

    companion object {
        private val log by Logger()
    }
}