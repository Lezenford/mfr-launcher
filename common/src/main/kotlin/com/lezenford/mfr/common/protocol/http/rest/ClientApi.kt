package com.lezenford.mfr.common.protocol.http.rest

import kotlinx.coroutines.flow.Flow
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpMethod
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import com.lezenford.mfr.common.extensions.WebClientSpec
import com.lezenford.mfr.common.extensions.toUri
import com.lezenford.mfr.common.extensions.toUriBuilder
import com.lezenford.mfr.common.extensions.withMethod
import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.Client
import com.lezenford.mfr.common.protocol.http.dto.Content
import java.time.LocalDateTime

interface ClientApi {
    @GetMapping(builds)
    suspend fun findAllBuild(): Flow<BuildDto>

    @GetMapping(build)
    suspend fun findBuild(
        @PathVariable id: Int,
        @RequestParam(
            required = false,
            value = "lastUpdate"
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) lastUpdate: LocalDateTime? = null
    ): Content

    @GetMapping(client)
    suspend fun clientVersions(): Flow<Client>

    companion object {
        fun findAllBuild(): WebClientSpec = builds.toUri().withMethod(HttpMethod.GET)

        fun findBuild(id: Int, lastUpdate: LocalDateTime? = null): WebClientSpec =
            build.toUriBuilder().apply {
                lastUpdate?.also { queryParam("lastUpdate", lastUpdate) }
            }.buildAndExpand(id).withMethod(HttpMethod.GET)

        fun clientVersions(): WebClientSpec = client.toUri().withMethod(HttpMethod.GET)

        private const val builds = "/api/v1/game"
        private const val build = "/api/v1/game/{id}"
        private const val client = "/api/v1/client"
    }
}