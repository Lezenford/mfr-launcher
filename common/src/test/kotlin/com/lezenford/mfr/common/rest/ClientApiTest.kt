package com.lezenford.mfr.common.rest

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.bind.annotation.RestController
import com.lezenford.mfr.common.extensions.spec
import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.Client
import com.lezenford.mfr.common.protocol.http.dto.Content
import com.lezenford.mfr.common.protocol.http.rest.ClientApi
import java.time.LocalDateTime

@EnableAutoConfiguration
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = [ClientApiTest.ClientApiImpl::class])
class ClientApiTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun `get all builds`() {
        webClient.spec(ClientApi.findAllBuild()).assert()
    }

    @Test
    fun `get build`() {
        webClient.spec(ClientApi.findBuild(1)).assert()
        webClient.spec(ClientApi.findBuild(1, LocalDateTime.now())).assert()
    }

    @Test
    fun `get client`() {
        webClient.spec(ClientApi.clientVersions()).assert()
    }

    private fun WebTestClient.RequestHeadersSpec<*>.assert() {
        exchange().expectStatus().is2xxSuccessful
    }

    @RestController
    class ClientApiImpl : ClientApi {
        override suspend fun findAllBuild(): Flow<BuildDto> = emptyFlow()
        override suspend fun findBuild(id: Int, lastUpdate: LocalDateTime?): Content = Content(emptyList())
        override suspend fun clientVersions(): Flow<Client> = emptyFlow()
    }
}