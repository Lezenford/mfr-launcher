package com.lezenford.mfr.common.rest

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.RestController
import com.lezenford.mfr.common.extensions.spec
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.LauncherDto
import com.lezenford.mfr.common.protocol.http.dto.Summary
import com.lezenford.mfr.common.protocol.http.rest.ServiceApi
import org.junit.jupiter.api.Disabled

@EnableAutoConfiguration
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = [ServiceApiTest.ServiceApiImpl::class])
class ServiceApiTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun `get all builds`() {
        webClient.spec(ServiceApi.builds()).assert()
    }

    @Test
    fun `create new build`() {
        webClient.spec(ServiceApi.createBuild("test", "test")).assert()
    }

    @Test
    fun `update build`() {
        webClient.spec(ServiceApi.updateBuild(1)).assert()
    }

    @Test
    fun `set build as default`() {
        webClient.spec(ServiceApi.setBuildDefault(1)).assert()
    }

    @Test
    fun `update manual`() {
        webClient.spec(ServiceApi.updateManual()).assert()
    }

    @Test
    fun `get launcher info`() {
        webClient.spec(ServiceApi.launcher(SystemType.WINDOWS)).assert()
    }

    @Test
    @Disabled
    fun `update launcher`() {
        webClient.spec(ServiceApi.updateLauncher(SystemType.WINDOWS, "test", "test")).assert()
        webClient.spec(ServiceApi.updateLauncher(SystemType.WINDOWS, "test")).assert()
    }

    @Test
    fun summary() {
        webClient.spec(ServiceApi.summary()).assert()
    }

    @Test
    fun maintenance() {
        webClient.spec(ServiceApi.maintenance(true)).assert()
    }

    private fun WebTestClient.RequestHeadersSpec<*>.assert() {
        exchange().expectStatus().is2xxSuccessful
    }

    @RestController
    class ServiceApiImpl : ServiceApi {
        override suspend fun builds(): Flow<BuildDto> = emptyFlow()
        override suspend fun createBuild(name: String, branch: String): ResponseEntity<Unit> =
            ResponseEntity.ok().build()

        override suspend fun updateBuild(buildId: Int) {}
        override suspend fun setBuildDefault(buildId: Int) {}
        override suspend fun updateManual() {}
        override suspend fun launcher(system: SystemType): LauncherDto = LauncherDto(SystemType.WINDOWS)
        override suspend fun updateLauncher(system: SystemType, version: String, name: String?, file: FilePart) {}
        override suspend fun summary(): Summary =
            Summary(Summary.Value(0, 0), Summary.Value(0, 0), Summary.Value(0, 0), emptyMap())

        override suspend fun maintenance(active: Boolean) {}
    }
}

