package com.lezenford.mfr.server.controller

import com.lezenford.mfr.server.component.ServerStatus
import com.lezenford.mfr.server.extensions.toBuildDto
import com.lezenford.mfr.server.extensions.toClient
import com.lezenford.mfr.server.extensions.toContent
import com.lezenford.mfr.server.service.model.BuildService
import com.lezenford.mfr.server.service.model.CategoryService
import com.lezenford.mfr.server.service.model.LauncherService
import com.lezenford.mfr.server.service.model.ReportService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.fullrest.mfr.common.IDENTITY_HEADER
import ru.fullrest.mfr.common.api.rest.BuildDto
import ru.fullrest.mfr.common.api.rest.CLIENT_PATH
import ru.fullrest.mfr.common.api.rest.Client
import ru.fullrest.mfr.common.api.rest.Content
import ru.fullrest.mfr.common.api.rest.GAME_PATH
import ru.fullrest.mfr.common.api.rest.REPORT_PATH
import ru.fullrest.mfr.common.api.rest.ReportDto
import java.time.LocalDateTime

@RestController
@RequestMapping("api/v1")
class ClientController(
    private val buildService: BuildService,
    private val categoryService: CategoryService,
    private val reportService: ReportService,
    private val launcherService: LauncherService
) {

    @GetMapping(GAME_PATH)
    fun findAllBuild(): List<BuildDto> = ServerStatus.operation {
        buildService.findAll().map { it.toBuildDto() }
    }

    @GetMapping("${GAME_PATH}/{id}")
    fun findBuild(
        @PathVariable id: Int,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) lastUpdate: LocalDateTime?
    ): Content = ServerStatus.operation {
        categoryService.findAllByBuildId(id).toContent(lastUpdate)
    }

    @PostMapping(REPORT_PATH)
    fun uploadReport(@RequestBody report: ReportDto, @RequestHeader(name = IDENTITY_HEADER) identity: String) {
        reportService.create(report, identity)
    }

    @GetMapping(CLIENT_PATH)
    fun clientVersion(): List<Client> = launcherService.findAll().map { it.toClient() }
}