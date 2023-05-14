package com.lezenford.mfr.server.controller.rest

import com.lezenford.mfr.common.extensions.spec
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.LauncherDto
import com.lezenford.mfr.common.protocol.http.rest.ServiceApi
import com.lezenford.mfr.server.BaseTest
import com.lezenford.mfr.server.model.entity.Build
import com.lezenford.mfr.server.model.entity.Launcher
import com.lezenford.mfr.server.model.repository.BuildRepository
import com.lezenford.mfr.server.model.repository.LauncherRepository
import com.lezenford.mfr.server.service.MaintenanceService
import com.lezenford.mfr.server.service.UpdaterService
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.core.io.FileUrlResource
import org.springframework.http.HttpStatus
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.function.BodyInserters
import java.util.*
import kotlin.io.path.absolutePathString

internal class ServiceControllerTest : BaseTest() {

    @Autowired
    private lateinit var webClient: WebTestClient

    @SpyBean
    private lateinit var buildRepository: BuildRepository

    @Autowired
    private lateinit var launcherRepository: LauncherRepository

    @SpyBean
    private lateinit var updaterService: UpdaterService

    @Autowired
    private lateinit var maintenanceService: MaintenanceService

    @BeforeEach
    fun setUp() {
        reset(buildRepository, updaterService)
    }

    @Test
    @WithMockUser
    fun `find builds`() {
        val build1 = Build(name = "build1", branch = "test1", default = true)
        val build2 = Build(name = "build2", branch = "test2")
        buildRepository.save(build1)
        buildRepository.save(build2)

        webClient.spec(ServiceApi.builds()).exchange().expectStatus().isOk
            .expectBody<List<BuildDto>>().returnResult().responseBody!!.also { builds ->
                assertThat(builds).hasSize(2)
                builds.forEach { build ->
                    val expectedBuild = listOf(build1, build2).first { it.name == build.name }
                    assertThat(build.default).isEqualTo(expectedBuild.default)
                    assertThat(build.lastUpdate.withNano(0)).isEqualTo(expectedBuild.lastUpdateDate.withNano(0))
                }
            }
    }

    @Test
    @WithMockUser
    fun `create build successfully`() {
        val name = UUID.randomUUID().toString()
        val branch = UUID.randomUUID().toString()
        webClient.spec(ServiceApi.createBuild(name, branch)).exchange().expectStatus().isCreated
        val build = buildRepository.findByName(name)!!
        assertThat(build.name).isEqualTo(name)
        assertThat(build.branch).isEqualTo(branch)
    }

    @Test
    @WithMockUser
    fun `conflict when create build`() {
        val name = UUID.randomUUID().toString()
        val branch = UUID.randomUUID().toString()
        webClient.spec(ServiceApi.createBuild(name, branch)).exchange().expectStatus().isCreated
        webClient.spec(ServiceApi.createBuild(name, branch)).exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT)
    }

    @Test
    @WithMockUser
    fun `error when create build`() {
        Mockito.doThrow(RuntimeException::class.java).`when`(buildRepository).save(any())

        webClient.spec(ServiceApi.createBuild(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
            .exchange().expectStatus().is5xxServerError
    }

    @Test
    @WithMockUser
    fun `successfully run update build`() {
        webClient.spec(ServiceApi.updateBuild(1)).exchange().expectStatus().isOk
        runBlocking {
            verify(updaterService, timeout(1000).only()).updateBuild(any())
        }
    }

    @Test
    @WithMockUser
    fun `successfully set default`() {
        var build1 = Build(name = "build1", branch = "test1", default = true)
        var build2 = Build(name = "build2", branch = "test2")
        buildRepository.save(build1)
        buildRepository.save(build2)

        webClient.spec(ServiceApi.setBuildDefault(build2.id)).exchange().expectStatus().isOk

        build1 = buildRepository.findById(build1.id).get()
        assertThat(build1.default).isFalse
        build2 = buildRepository.findById(build2.id).get()
        assertThat(build2.default).isTrue
    }

    @Test
    @WithMockUser
    fun `successfully run update manual`() {
        webClient.spec(ServiceApi.updateManual()).exchange().expectStatus().isOk
        runBlocking {
            verify(updaterService, timeout(1000).only()).updateManual()
        }
    }

    @Test
    @WithMockUser
    fun `successfully find launcher`() {
        val launcher = Launcher(system = SystemType.WINDOWS, version = "1.0.0", fileName = "test")
        launcherRepository.save(launcher)

        webClient.spec(ServiceApi.launcher(launcher.system)).exchange().expectStatus().isOk
            .expectBody<LauncherDto>().returnResult().responseBody!!.also {
                assertThat(it.type).isEqualTo(launcher.system)
                assertThat(it.version).isEqualTo(launcher.version)
                assertThat(it.fileName).isEqualTo(launcher.fileName)
            }
    }

    @Test
    @WithMockUser
    fun `successfully return not prepared launcher info`() {
        webClient.spec(ServiceApi.launcher(SystemType.MACOS)).exchange().expectStatus().isOk
            .expectBody<LauncherDto>().returnResult().responseBody!!.also {
                assertThat(it.type).isEqualTo(SystemType.MACOS)
                assertThat(it.version).isNull()
                assertThat(it.fileName).isNull()
            }
    }

    @Test
    @WithMockUser
    fun `successfully run update launcher`() {
        runBlocking {
            Mockito.doReturn(Unit).`when`(updaterService).updateLauncher(any(), any(), any(), any())
            val multipartBodyBuilder = MultipartBodyBuilder()
            multipartBodyBuilder.part("file", FileUrlResource(createTempFile().absolutePathString()))
            webClient.spec(ServiceApi.updateLauncher(SystemType.WINDOWS, "1.0.1", name = "test"))
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                .exchange().expectStatus().isOk

            verify(updaterService, timeout(1000).only()).updateLauncher(any(), any(), any(), any())
        }
    }

    @Test
    @WithMockUser
    fun `successfully get summary`() {
        webClient.spec(ServiceApi.summary()).exchange().expectStatus().isOk
    }

    @Test
    @WithMockUser
    fun `check maintenance change`() {
        assertThat(maintenanceService.maintenance(MaintenanceService.Type.ALL)).isFalse

        webClient.spec(ServiceApi.maintenance(true)).exchange().expectStatus().isOk

        assertThat(maintenanceService.maintenance(MaintenanceService.Type.ALL)).isTrue

        webClient.spec(ServiceApi.maintenance(false)).exchange().expectStatus().isOk

        assertThat(maintenanceService.maintenance(MaintenanceService.Type.ALL)).isFalse
    }
}