package com.lezenford.mfr.server.controller.rest

import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.Client
import com.lezenford.mfr.common.protocol.http.dto.Content
import com.lezenford.mfr.common.protocol.http.rest.ClientApi
import com.lezenford.mfr.server.annotation.Available
import com.lezenford.mfr.server.extensions.toBuildDto
import com.lezenford.mfr.server.extensions.toClient
import com.lezenford.mfr.server.extensions.toContent
import com.lezenford.mfr.server.service.MaintenanceService
import com.lezenford.mfr.server.service.model.BuildService
import com.lezenford.mfr.server.service.model.CategoryService
import com.lezenford.mfr.server.service.model.LauncherService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class RestClientController(
    private val buildService: BuildService,
    private val categoryService: CategoryService,
    private val launcherService: LauncherService
) : ClientApi {

    @Available(MaintenanceService.Type.GAME)
    override suspend fun findAllBuild(): Flow<BuildDto> = buildService.findAll().map { it.toBuildDto() }

    @Available(MaintenanceService.Type.GAME)
    override suspend fun findBuild(id: Int, lastUpdate: LocalDateTime?): Content =
        categoryService.findAllByBuildId(id).toContent(lastUpdate)

    @Available(MaintenanceService.Type.LAUNCHER)
    override suspend fun clientVersions(): Flow<Client> = launcherService.findAll().asFlow().map { it.toClient() }
}