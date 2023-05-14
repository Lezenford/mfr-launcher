package com.lezenford.mfr.server.controller.rest

import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.LauncherDto
import com.lezenford.mfr.common.protocol.http.dto.Summary
import com.lezenford.mfr.common.protocol.http.rest.ServiceApi
import com.lezenford.mfr.server.extensions.toBuildDto
import com.lezenford.mfr.server.model.entity.Build
import com.lezenford.mfr.server.service.MaintenanceService
import com.lezenford.mfr.server.service.UpdaterService
import com.lezenford.mfr.server.service.model.BuildService
import com.lezenford.mfr.server.service.model.LauncherService
import com.lezenford.mfr.server.service.model.SummaryService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceController(
    private val updaterService: UpdaterService,
    private val summaryService: SummaryService,
    private val buildService: BuildService,
    private val launcherService: LauncherService,
    private val maintenanceService: MaintenanceService
) : ServiceApi {

    override suspend fun builds(): Flow<BuildDto> = buildService.findAll().map { it.toBuildDto() }

    override suspend fun createBuild(name: String, branch: String): ResponseEntity<Unit> {
        return kotlin.runCatching {
            buildService.save(Build(name = name, branch = branch))
            ResponseEntity.status(HttpStatus.CREATED).build<Unit>()
        }.recover {
            if (it is DataIntegrityViolationException) {
                ResponseEntity.status(HttpStatus.CONFLICT).build()
            } else {
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            }
        }.getOrThrow()
    }

    override suspend fun updateBuild(buildId: Int) {
        CoroutineScope(Dispatchers.IO).launch { updaterService.updateBuild(buildId) }
    }

    override suspend fun setBuildDefault(buildId: Int) {
        buildService.updateDefault(buildId)
    }

    override suspend fun updateManual() {
        CoroutineScope(Dispatchers.IO).launch { updaterService.updateManual() }
    }

    override suspend fun launcher(system: SystemType): LauncherDto = launcherService.findBySystem(system)
        .let { LauncherDto(type = system, version = it?.version, fileName = it?.fileName) }

    override suspend fun updateLauncher(system: SystemType, version: String, name: String?, file: FilePart) {
        updaterService.updateLauncher(system, file, version, name)
    }

    override suspend fun summary(): Summary = summaryService.summary()

    override suspend fun maintenance(active: Boolean) {
        if (active) {
            maintenanceService.setUp(MaintenanceService.Type.ALL)
        } else {
            maintenanceService.setDown(MaintenanceService.Type.ALL)
        }
    }
}