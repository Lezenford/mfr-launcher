package com.lezenford.mfr.launcher.service.provider

import com.lezenford.mfr.common.extensions.spec
import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.Client
import com.lezenford.mfr.common.protocol.http.dto.Content
import com.lezenford.mfr.common.protocol.http.rest.ClientApi
import com.lezenford.mfr.launcher.exception.ServerConnectionException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.util.retry.Retry
import java.io.IOException
import java.time.LocalDateTime

@Component
class RestProvider(
    private val webClient: WebClient
) : ClientApi {
    override suspend fun findAllBuild(): Flow<BuildDto> {
        return webClient.spec(ClientApi.findAllBuild())
            .retrieve()
            .onStatus({ it == HttpStatus.SERVICE_UNAVAILABLE }) { mono { com.lezenford.mfr.common.exception.ServerMaintenanceException() } }
            .bodyToFlow<BuildDto>()
            .retry(3) { it != com.lezenford.mfr.common.exception.ServerMaintenanceException::class }
            .catch {
                when (it) {
                    is IOException -> {
                        throw ServerConnectionException(it)
                    }

                    else -> throw it
                }
            }
    }

    override suspend fun findBuild(id: Int, lastUpdate: LocalDateTime?): Content {
        return webClient.spec(ClientApi.findBuild(id, lastUpdate))
            .retrieve()
            .onStatus({ it == HttpStatus.SERVICE_UNAVAILABLE }) { mono { com.lezenford.mfr.common.exception.ServerMaintenanceException() } }
            .bodyToMono<Content>()
            .retryWhen(Retry.max(3).filter { it != com.lezenford.mfr.common.exception.ServerMaintenanceException::class })
            .doOnError {
                when (it) {
                    is IOException -> {
                        throw ServerConnectionException(it)
                    }

                    else -> throw it
                }
            }
            .awaitSingle()
    }

    override suspend fun clientVersions(): Flow<Client> {
        return webClient.spec(ClientApi.clientVersions())
            .retrieve()
            .onStatus({ it == HttpStatus.SERVICE_UNAVAILABLE }) { mono { com.lezenford.mfr.common.exception.ServerMaintenanceException() } }
            .bodyToFlow<Client>()
            .retry(3) { it != com.lezenford.mfr.common.exception.ServerMaintenanceException::class }
            .catch {
                when (it) {
                    is IOException -> {
                        throw ServerConnectionException(it)
                    }

                    else -> throw it
                }
            }
    }
}