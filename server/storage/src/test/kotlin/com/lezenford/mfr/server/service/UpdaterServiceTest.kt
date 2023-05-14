package com.lezenford.mfr.server.service

import com.lezenford.mfr.server.BaseTest
import com.lezenford.mfr.server.model.repository.LauncherRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileUrlResource
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import com.lezenford.mfr.common.extensions.md5
import com.lezenford.mfr.common.extensions.spec
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.http.rest.ServiceApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeBytes
import kotlin.random.Random

@TestPropertySource(properties = ["setting.launcherFolder=\${UpdaterServiceTest.launcherFolder}"])
internal class UpdaterServiceTest : BaseTest() {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var launcherRepository: LauncherRepository

    @Test
    @WithMockUser
    fun `upload new launcher`() {
        val launcherFileSource = createTempFile(SOURCE_FOLDER)
        launcherFileSource.writeBytes(Random.nextBytes(1024 * 1024))

        val multipartBodyBuilder = MultipartBodyBuilder()
        multipartBodyBuilder.part("file", FileUrlResource(launcherFileSource.absolutePathString()))
        val version = "1.0.0"
        val name = "testLauncher"
        webClient.spec(ServiceApi.updateLauncher(SystemType.WINDOWS, version = version, name = name))
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build())).exchange().expectStatus().isOk

        val launcher = launcherRepository.findBySystem(SystemType.WINDOWS)!!
        assertThat(launcher.version).isEqualTo(version)
        assertThat(launcher.fileName).isEqualTo(name)

        val launcherFile = LAUNCHER_FOLDER.resolve(launcher.system.name).resolve(launcher.fileName)
        assertThat(launcherFile).exists()
        assertThat(launcherFile.md5().contentEquals(launcher.md5)).isTrue
        assertThat(launcherFile.md5().contentEquals(launcherFileSource.md5())).isTrue
    }

    @Test
    @WithMockUser
    fun `update exist launcher`() {
        var launcherFileSource = createTempFile(SOURCE_FOLDER)
        launcherFileSource.writeBytes(Random.nextBytes(1024 * 1024))

        var multipartBodyBuilder = MultipartBodyBuilder()
        multipartBodyBuilder.part("file", FileUrlResource(launcherFileSource.absolutePathString()))
        var version = "1.0.0"
        val name = "testLauncher"
        webClient.spec(ServiceApi.updateLauncher(SystemType.WINDOWS, version = version, name = name))
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build())).exchange().expectStatus().isOk


        launcherFileSource = createTempFile(SOURCE_FOLDER)
        launcherFileSource.writeBytes(Random.nextBytes(1024 * 1024))

        multipartBodyBuilder = MultipartBodyBuilder()
        multipartBodyBuilder.part("file", FileUrlResource(launcherFileSource.absolutePathString()))

        version = "1.0.1"
        webClient.spec(ServiceApi.updateLauncher(SystemType.WINDOWS, version = version))
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build())).exchange().expectStatus().isOk

        val launcher = launcherRepository.findBySystem(SystemType.WINDOWS)!!
        assertThat(launcher.version).isEqualTo(version)
        assertThat(launcher.fileName).isEqualTo(name)

        val launcherFile = LAUNCHER_FOLDER.resolve(launcher.system.name).resolve(launcher.fileName)
        assertThat(launcherFile).exists()
        assertThat(launcherFile.md5().contentEquals(launcher.md5)).isTrue
        assertThat(launcherFile.md5().contentEquals(launcherFileSource.md5())).isTrue
    }

    @Test
    @WithMockUser
    fun `error upload new launcher without name`() {
        val launcherFileSource = createTempFile(SOURCE_FOLDER)
        launcherFileSource.writeBytes(Random.nextBytes(1024 * 1024))

        val multipartBodyBuilder = MultipartBodyBuilder()
        multipartBodyBuilder.part("file", FileUrlResource(launcherFileSource.absolutePathString()))
        val version = "1.0.0"
        webClient.spec(ServiceApi.updateLauncher(SystemType.WINDOWS, version = version))
            .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build())).exchange()
            .expectStatus().is5xxServerError
    }

    companion object {
        private val LAUNCHER_FOLDER = createTempDirectory()
        private val SOURCE_FOLDER = createTempDirectory()

        @BeforeAll
        @JvmStatic
        fun prepareProperties() {
            System.setProperty("UpdaterServiceTest.launcherFolder", LAUNCHER_FOLDER.absolutePathString())
        }
    }
}