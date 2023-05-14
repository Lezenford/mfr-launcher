@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lezenford.mfr.manager.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.lezenford.mfr.manager.BaseTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClientResponseException
import com.lezenford.mfr.common.SESSION_ID
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.http.dto.LauncherDto
import com.lezenford.mfr.common.protocol.http.dto.Summary

internal class StorageServiceTest : BaseTest() {

    @Autowired
    private lateinit var storageService: StorageService

    @Autowired
    private lateinit var mockWebServer: MockWebServer

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `find all builds`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("[]")
        )
        assertThat(storageService.findAllBuilds().count()).isEqualTo(0)
        val request = mockWebServer.takeRequest()

        assertThat(request.headers.first { it.first == HttpHeaders.COOKIE }.second).startsWith(SESSION_ID)
    }

    @Test
    fun `find all builds when storage server is unavailable`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
        )

        assertThrows<WebClientResponseException> { storageService.findAllBuilds().collect() }

        val request = mockWebServer.takeRequest()

        assertThat(request.headers.first { it.first == HttpHeaders.COOKIE }.second).startsWith(SESSION_ID)
    }

    @Test
    fun `find launcher`() = runTest {
        val launcherDto = LauncherDto(SystemType.WINDOWS, "1.0.0", "launcher")
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(launcherDto))
        )
        assertThat(storageService.findLauncher(SystemType.WINDOWS)).isEqualTo(launcherDto)
    }

    @Test
    fun `find launcher with error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
        )
        assertThrows<WebClientResponseException> { storageService.findLauncher(SystemType.WINDOWS) }
    }

    @Test
    fun `find summary`() = runTest {
        val summary = Summary(
            users = Summary.Value(0, 0),
            gameDownloads = Summary.Value(10, 10),
            extraContentDownload = Summary.Value(20, 20),
            optionalContentDownload = mapOf(
                "first" to Summary.Value(30, 30),
                "second" to Summary.Value(40, 40)
            )
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(objectMapper.writeValueAsString(summary))
        )
        assertThat(storageService.summary()).isEqualTo(summary)
    }

    @Test
    fun `find summary with error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
        )
        assertThrows<WebClientResponseException> { storageService.summary() }
    }

    @Test
    fun `take SUCCESS result`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        assertThat(storageService.createBuild("testName", "testBranch")).isEqualTo(StorageService.Result.SUCCESS)
    }

    @Test
    fun `take CONFLICT result`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(409))
        assertThat(storageService.createBuild("testName", "testBranch")).isEqualTo(StorageService.Result.CONFLICT)
    }

    @Test
    fun `take ERROR result`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        assertThat(storageService.createBuild("testName", "testBranch")).isEqualTo(StorageService.Result.ERROR)
    }
}