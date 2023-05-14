package com.lezenford.mfr.manager.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.bodyToMono
import com.lezenford.mfr.common.SESSION_ID
import com.lezenford.mfr.common.extensions.Logger
import com.lezenford.mfr.common.extensions.spec
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.LauncherDto
import com.lezenford.mfr.common.protocol.http.dto.Summary
import com.lezenford.mfr.common.protocol.http.rest.ServiceApi

@Service
class StorageService(
    private val storageServiceWebClient: WebClient,
    private val jwtService: JwtService
) {
    suspend fun findAllBuilds(): Flow<BuildDto> = storageServiceWebClient.spec(ServiceApi.builds())
        .header(HttpHeaders.COOKIE, "${SESSION_ID}=${jwtService.generateToken()}")
        .retrieve()
        .bodyToFlow()

    suspend fun createBuild(name: String, branch: String): Result =
        storageServiceWebClient.spec(ServiceApi.createBuild(name, branch)).request()

    suspend fun updateBuild(id: Int): Result = storageServiceWebClient.spec(ServiceApi.updateBuild(id)).request()

    suspend fun setDefaultBuild(id: Int): Result =
        storageServiceWebClient.spec(ServiceApi.setBuildDefault(id)).request()

    suspend fun findLauncher(system: SystemType): LauncherDto =
        storageServiceWebClient.spec(ServiceApi.launcher(system))
            .header(HttpHeaders.COOKIE, "${com.lezenford.mfr.common.SESSION_ID}=${jwtService.generateToken()}")
            .retrieve()
            .awaitBody()

    suspend fun updateLauncher(system: SystemType, version: String, fileName: String? = null): Result =
        storageServiceWebClient.spec(ServiceApi.updateLauncher(system, version, fileName)).request()

    suspend fun updateManual(): Result = storageServiceWebClient.spec(ServiceApi.updateManual()).request()

    suspend fun summary(): Summary = storageServiceWebClient.spec(ServiceApi.summary())
        .header(HttpHeaders.COOKIE, "${com.lezenford.mfr.common.SESSION_ID}=${jwtService.generateToken()}")
        .retrieve()
        .awaitBody()

    private suspend fun WebClient.RequestHeadersSpec<*>.request(): Result =
        header(HttpHeaders.COOKIE, "${com.lezenford.mfr.common.SESSION_ID}=${jwtService.generateToken()}")
            .awaitExchange { it }
            .let {
                when (it.statusCode()) {
                    HttpStatus.OK -> Result.SUCCESS
                    HttpStatus.CONFLICT -> Result.CONFLICT
                    else -> Result.ERROR.apply {
                        log.error("Request with error: ${it.statusCode()}", it.createException().awaitSingle())
                    }
                }
            }

    enum class Result {
        SUCCESS, CONFLICT, ERROR
    }

    companion object {
        private val log by Logger()
    }
}