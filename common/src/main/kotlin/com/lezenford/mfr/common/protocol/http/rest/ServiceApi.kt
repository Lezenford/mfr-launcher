package com.lezenford.mfr.common.protocol.http.rest

import kotlinx.coroutines.flow.Flow
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import com.lezenford.mfr.common.extensions.WebClientSpec
import com.lezenford.mfr.common.extensions.toUri
import com.lezenford.mfr.common.extensions.toUriBuilder
import com.lezenford.mfr.common.extensions.withMethod
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.LauncherDto
import com.lezenford.mfr.common.protocol.http.dto.Summary

interface ServiceApi {

    @GetMapping(builds)
    suspend fun builds(): Flow<BuildDto>

    @PostMapping(builds)
    suspend fun createBuild(@RequestParam name: String, @RequestParam branch: String): ResponseEntity<Unit>

    @PutMapping(updateBuild)
    suspend fun updateBuild(@PathVariable(value = "id") buildId: Int)

    @PatchMapping(updateBuild)
    suspend fun setBuildDefault(@PathVariable(value = "id") buildId: Int)

    @PutMapping(manual)
    suspend fun updateManual()

    @GetMapping(launcher)
    suspend fun launcher(@PathVariable system: SystemType): LauncherDto

    @PutMapping(launcher, consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    suspend fun updateLauncher(
        @PathVariable system: SystemType,
        @RequestParam version: String,
        @RequestParam(required = false) name: String?,
        @RequestPart file: FilePart
    )

    @GetMapping(summary)
    suspend fun summary(): Summary

    @PutMapping(maintenance)
    suspend fun maintenance(@RequestParam active: Boolean)

    companion object {
        fun builds(): WebClientSpec = builds.toUri().withMethod(HttpMethod.GET)

        fun createBuild(name: String, branch: String): WebClientSpec = builds.toUriBuilder()
            .queryParam("name", name).queryParam("branch", branch).build()
            .withMethod(HttpMethod.POST)

        fun updateBuild(id: Int): WebClientSpec =
            updateBuild.toUriBuilder().buildAndExpand(id).withMethod(HttpMethod.PUT)

        fun updateManual(): WebClientSpec = manual.toUri().withMethod(HttpMethod.PUT)

        fun setBuildDefault(id: Int): WebClientSpec =
            updateBuild.toUriBuilder().buildAndExpand(id).withMethod(HttpMethod.PATCH)

        fun launcher(system: SystemType): WebClientSpec =
            launcher.toUriBuilder().buildAndExpand(system).withMethod(HttpMethod.GET)

        fun updateLauncher(system: SystemType, version: String, name: String? = null): WebClientSpec =
            launcher.toUriBuilder()
                .queryParam("version", version).queryParam("name", name).buildAndExpand(system)
                .withMethod(HttpMethod.PUT, MediaType.MULTIPART_FORM_DATA)

        fun summary(): WebClientSpec = summary.toUri().withMethod(HttpMethod.GET)

        fun maintenance(active: Boolean): WebClientSpec =
            maintenance.toUriBuilder().queryParam("active", active).build().withMethod(HttpMethod.PUT)

        private const val builds: String = "/service/build"
        private const val updateBuild: String = "/service/build/{id}"
        private const val manual: String = "/service/manual"
        private const val launcher: String = "/service/launcher/{system}"
        private const val summary: String = "/service/summary"
        private const val maintenance: String = "/service/maintenance"
    }
}