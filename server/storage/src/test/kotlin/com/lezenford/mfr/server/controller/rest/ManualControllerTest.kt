package com.lezenford.mfr.server.controller.rest

import com.lezenford.mfr.server.BaseTest
import com.lezenford.mfr.server.service.MaintenanceService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

@TestPropertySource(properties = ["setting.manual.local=\${ManualControllerTest.manualFolder}"])
internal class ManualControllerTest : BaseTest() {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var maintenanceService: MaintenanceService

    @BeforeEach
    fun setUp() {
        MaintenanceService.Type.values().forEach { maintenanceService.setDown(it) }
    }

    @Test
    fun `successfully redirect for full path`() {
        var startUrl = "/"
        var redirectLocation = webClient.get().uri(startUrl).exchange()
            .expectStatus().isTemporaryRedirect.returnResult<Unit>().responseHeaders[HttpHeaders.LOCATION]!!.first()
        webClient.get().uri("/$redirectLocation").exchange().expectStatus().isOk.expectBody<String>()
            .isEqualTo(INDEX_PAGE_CONTENT)

        startUrl = "/readme"
        redirectLocation = webClient.get().uri(startUrl).exchange()
            .expectStatus().isTemporaryRedirect.returnResult<Unit>().responseHeaders[HttpHeaders.LOCATION]!!.first()
        webClient.get().uri("/$redirectLocation").exchange().expectStatus().isOk.expectBody<String>()
            .isEqualTo(INDEX_PAGE_CONTENT)
    }

    @Test
    fun `successfully redirect for relative path`() {
        val startUrl = "/readme/"
        val redirectLocation = webClient.get().uri(startUrl).exchange()
            .expectStatus().isTemporaryRedirect.returnResult<Unit>().responseHeaders[HttpHeaders.LOCATION]!!.first()
        webClient.get().uri("$startUrl$redirectLocation").exchange().expectStatus().isOk.expectBody<String>()
            .isEqualTo(INDEX_PAGE_CONTENT)
    }

    @Test
    fun `decline while manual maintenance active`() {
        maintenanceService.setUp(MaintenanceService.Type.MANUAL)
        webClient.get().uri("/").exchange().expectStatus().is5xxServerError
        webClient.get().uri("/readme").exchange().expectStatus().is5xxServerError
        webClient.get().uri("/readme/").exchange().expectStatus().is5xxServerError
    }

    @Test
    fun `allow while not manual maintenance active`() {
        maintenanceService.setUp(MaintenanceService.Type.GAME)
        webClient.get().uri("/").exchange().expectStatus().is3xxRedirection
        webClient.get().uri("/readme").exchange().expectStatus().is3xxRedirection
        webClient.get().uri("/readme/").exchange().expectStatus().is3xxRedirection
    }


    companion object {
        private val TEMP_MANUAL_FOLDER = createTempDirectory()
        private const val INDEX_PAGE_CONTENT = "Index page"

        @BeforeAll
        @JvmStatic
        fun prepareSettings() {
            System.setProperty("ManualControllerTest.manualFolder", TEMP_MANUAL_FOLDER.absolutePathString())
            val indexPage = Files.createFile(TEMP_MANUAL_FOLDER.resolve("index.html"))
            indexPage.writeText(INDEX_PAGE_CONTENT)
        }
    }
}