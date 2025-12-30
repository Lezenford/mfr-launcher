package com.lezenford.mfr.launcher.service.provider

import com.lezenford.mfr.launcher.config.properties.ApplicationProperties
import com.lezenford.mfr.launcher.model.dto.Version
import com.lezenford.mfr.launcher.service.Location
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.model.VersionSchemaResponse
import com.lezenford.mfr.schema.v1.Schema
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.readBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import org.springframework.stereotype.Component
import kotlin.system.measureTimeMillis

@Component
class KtorProvider(
    private val client: HttpClient,
    private val properties: ApplicationProperties
) {
    private val host: String
        get() = when (State.location.value) {
            Location.RU -> properties.server.ruLocation.address
            Location.EU -> properties.server.euLocation.address
        }.let { "https://$it" }

    suspend fun findActiveGameVersion(): String {
        return client.get("$host/v1/game/version") {
            header(CLIENT_ID_HEADER, State.clientId.value)
            parameter("os", "WINDOWS")
        }.call.response.body<Version>().id
    }

    suspend fun findGameVersionSchema(version: String): VersionSchemaResponse {
        return client.get("$host/v1/game/files") {
            header(CLIENT_ID_HEADER, State.clientId.value)
            parameter("os", "WINDOWS")
            parameter("version", version)
            parameter("region", State.location.value)
        }.call.response.body<VersionSchemaResponse>()
    }

    suspend fun findGameSchema(host: String, path: String): Schema {
        return Schema.parseFrom(client.get("${host}/${path}"){
            header(CLIENT_ID_HEADER, State.clientId.value)
        }.call.response.readBytes())
    }

    suspend fun findGameFilesPlan(host: String, path: String): com.lezenford.mfr.version.v1.Version {
        return com.lezenford.mfr.version.v1.Version.parseFrom(client.get("${host}/${path}"){
            header(CLIENT_ID_HEADER, State.clientId.value)
        }.call.response.readBytes())
    }

    suspend fun findActiveLauncherVersion(): String {
        return client.get("${host}/v1/launcher/version") {
            header(CLIENT_ID_HEADER, State.clientId.value)
            parameter("os", "WINDOWS")
        }.call.response.body<Version>().id
    }

    suspend fun findLauncherVersionSchema(version: String): VersionSchemaResponse {
        return client.get("$host/v1/launcher/files") {
            parameter("os", "WINDOWS")
            parameter("version", version)
            parameter("region", State.location.value)
        }.call.response.body<VersionSchemaResponse>()
    }

    suspend fun downloadFile(host: String, path: String): ByteReadChannel {
        val response = client.get("$host/$path").call.response
        return when {
            response.status == HttpStatusCode.RequestedRangeNotSatisfiable -> ByteReadChannel.Empty
            response.status.isSuccess() -> response.bodyAsChannel()
            else -> throw IllegalArgumentException("Request return error code ${response.status}")
        }
    }

    suspend fun checkAvailable(host: String): Int? {
        return runCatching {
            measureTimeMillis {
                client.get("$host/ping") {
                    timeout {
                        requestTimeoutMillis = 5_000
                        connectTimeoutMillis = 5_000
                        socketTimeoutMillis = 5_000
                    }
                }.call.takeIf { it.response.status.isSuccess() }
                    ?: throw IllegalArgumentException("Ping failed to $host")
            }.toInt()
        }.getOrNull()
    }

    companion object {
        private const val  CLIENT_ID_HEADER = "X-Client-ID"
    }
}