package com.lezenford.mfr.server.controller.rest

import com.lezenford.mfr.common.extensions.spec
import com.lezenford.mfr.common.protocol.enums.ContentType
import com.lezenford.mfr.common.protocol.enums.SystemType
import com.lezenford.mfr.common.protocol.http.dto.BuildDto
import com.lezenford.mfr.common.protocol.http.dto.Client
import com.lezenford.mfr.common.protocol.http.dto.Content
import com.lezenford.mfr.common.protocol.http.rest.ClientApi
import com.lezenford.mfr.server.BaseTest
import com.lezenford.mfr.server.model.entity.*
import com.lezenford.mfr.server.service.MaintenanceService
import com.lezenford.mfr.server.service.model.BuildService
import com.lezenford.mfr.server.service.model.CategoryService
import com.lezenford.mfr.server.service.model.LauncherService
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.LocalDateTime

internal class ClientControllerTest : BaseTest() {
    @MockBean
    private lateinit var buildService: BuildService

    @MockBean
    private lateinit var categoryService: CategoryService

    @MockBean
    private lateinit var launcherService: LauncherService

    @Autowired
    private lateinit var webClient: WebTestClient

    @Autowired
    private lateinit var maintenanceService: MaintenanceService

    @Test
    fun `find all builds successfully`() {
        Mockito.doReturn(flowOf(BUILD_1, BUILD_2)).`when`(buildService).findAll()

        webClient.spec(ClientApi.findAllBuild()).exchange()
            .expectStatus().isOk
            .expectBody<List<BuildDto>>().consumeWith { entity ->
                assertThat(entity.responseBody).hasSize(2)
                entity.responseBody!!.apply {
                    assertThat(this).anyMatch { it.id == BUILD_1.id }
                    assertThat(this).anyMatch { it.id == BUILD_2.id }
                    assertThat(this).anyMatch { it.name == BUILD_1.name }
                    assertThat(this).anyMatch { it.name == BUILD_2.name }
                    assertThat(this).anyMatch { it.lastUpdate == BUILD_1.lastUpdateDate }
                    assertThat(this).anyMatch { it.lastUpdate == BUILD_2.lastUpdateDate }
                    assertThat(this).anyMatch { it.default == BUILD_1.default }
                    assertThat(this).anyMatch { it.default == BUILD_2.default }
                }
            }
    }

    @Test
    fun `find all builds successfully with not current maintenance`() {
        Mockito.doReturn(flowOf(BUILD_1, BUILD_2)).`when`(buildService).findAll()
        maintenanceService.setUp(MaintenanceService.Type.LAUNCHER)
        webClient.spec(ClientApi.findAllBuild()).exchange()
            .expectStatus().isOk
        maintenanceService.setDown(MaintenanceService.Type.LAUNCHER)
        maintenanceService.setUp(MaintenanceService.Type.MANUAL)
        webClient.spec(ClientApi.findAllBuild()).exchange()
            .expectStatus().isOk
        maintenanceService.setDown(MaintenanceService.Type.MANUAL)
    }

    @Test
    fun `find all builds with maintenance error`() {
        maintenanceService.setUp(MaintenanceService.Type.GAME)
        webClient.spec(ClientApi.findAllBuild()).exchange().expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        maintenanceService.setDown(MaintenanceService.Type.GAME)
        maintenanceService.setUp(MaintenanceService.Type.ALL)
        webClient.spec(ClientApi.findAllBuild()).exchange().expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        maintenanceService.setDown(MaintenanceService.Type.ALL)
    }

    @Test
    fun `find all build content`() {
        Mockito.doReturn(listOf(MAIN_CATEGORY_BUILD_1, EXTRA_CATEGORY_BUILD_1)).`when`(categoryService)
            .findAllByBuildId(anyInt())

        webClient.spec(ClientApi.findBuild(BUILD_1.id)).exchange().expectStatus().isOk.expectBody<Content>()
            .consumeWith { entity ->
                entity.responseBody!!.apply {
                    assertThat(categories).hasSize(2)
                    categories.forEach { category ->
                        val testCategory =
                            listOf(MAIN_CATEGORY_BUILD_1, EXTRA_CATEGORY_BUILD_1).first { it.type == category.type }
                        assertThat(category.required).isEqualTo(testCategory.required)
                        assertThat(category.items.size).isEqualTo(testCategory.items.size)
                        category.items.forEach { item ->
                            val testItem = testCategory.items.first { it.name == item.name }
                            assertThat(item.files).hasSize(testItem.files.size)
                            item.files.forEach { file ->
                                val testFile = testItem.files.first { it.id == file.id }
                                assertThat(file.active).isEqualTo(testFile.active)
                                assertThat(file.path).isEqualTo(testFile.path)
                                assertThat(file.size).isEqualTo(testFile.size)
                                assertThat(file.md5.contentEquals(testFile.md5)).isTrue
                            }
                        }
                    }
                }
            }
    }

    @Test
    fun `find all build content with not current maintenance`() {
        maintenanceService.setUp(MaintenanceService.Type.MANUAL)
        Mockito.doReturn(listOf(MAIN_CATEGORY_BUILD_1, EXTRA_CATEGORY_BUILD_1)).`when`(categoryService)
            .findAllByBuildId(anyInt())
        webClient.spec(ClientApi.findBuild(BUILD_1.id)).exchange().expectStatus().isOk
        maintenanceService.setDown(MaintenanceService.Type.MANUAL)
        maintenanceService.setUp(MaintenanceService.Type.LAUNCHER)
        Mockito.doReturn(listOf(MAIN_CATEGORY_BUILD_1, EXTRA_CATEGORY_BUILD_1)).`when`(categoryService)
            .findAllByBuildId(anyInt())
        webClient.spec(ClientApi.findBuild(BUILD_1.id)).exchange().expectStatus().isOk
        maintenanceService.setDown(MaintenanceService.Type.LAUNCHER)
    }

    @Test
    fun `find all build content after date`() {
        Mockito.doReturn(listOf(MAIN_CATEGORY_BUILD_1, EXTRA_CATEGORY_BUILD_1)).`when`(categoryService)
            .findAllByBuildId(anyInt())

        val lastUpdate = LocalDateTime.now().minusDays(1)
        webClient.spec(ClientApi.findBuild(BUILD_1.id, lastUpdate)).exchange()
            .expectStatus().isOk.expectBody<Content>()
            .consumeWith { entity ->
                entity.responseBody!!.apply {
                    assertThat(categories).hasSize(2)
                    categories.forEach { category ->
                        val testCategory =
                            listOf(MAIN_CATEGORY_BUILD_1, EXTRA_CATEGORY_BUILD_1).first { it.type == category.type }
                        assertThat(category.required).isEqualTo(testCategory.required)
                        assertThat(category.items.size).isEqualTo(testCategory.items.size)
                        category.items.forEach { item ->
                            val testItem = testCategory.items.first { it.name == item.name }
                            assertThat(item.files).hasSize(testItem.files.count { it.lastChangeDate.isAfter(lastUpdate) })
                            item.files.forEach { file ->
                                val testFile = testItem.files.first { it.id == file.id }
                                assertThat(testFile.lastChangeDate).isAfter(lastUpdate)
                                assertThat(file.active).isEqualTo(testFile.active)
                                assertThat(file.path).isEqualTo(testFile.path)
                                assertThat(file.size).isEqualTo(testFile.size)
                                assertThat(file.md5.contentEquals(testFile.md5)).isTrue
                            }
                        }
                    }
                }
            }
    }

    @Test
    fun `find all build content with maintenance error`() {
        maintenanceService.setUp(MaintenanceService.Type.GAME)
        webClient.spec(ClientApi.findBuild(1)).exchange().expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        maintenanceService.setDown(MaintenanceService.Type.GAME)
        maintenanceService.setUp(MaintenanceService.Type.ALL)
        webClient.spec(ClientApi.findBuild(1)).exchange().expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
        maintenanceService.setDown(MaintenanceService.Type.ALL)
    }

    @Test
    fun `find all launchers`() {
        Mockito.doReturn(listOf(LAUNCHER_MAC, LAUNCHER_WIN, LAUNCHER_LINUX)).`when`(launcherService).findAll()
        webClient.spec(ClientApi.clientVersions()).exchange().expectStatus().isOk.expectBody<List<Client>>()
            .consumeWith { entity ->
                entity.responseBody!!.forEach { client ->
                    val testClient =
                        listOf(LAUNCHER_WIN, LAUNCHER_MAC, LAUNCHER_LINUX).first { it.system == client.system }
                    assertThat(client.md5.contentEquals(testClient.md5)).isTrue
                    assertThat(client.size).isEqualTo(testClient.size)
                    assertThat(client.version).isEqualTo(testClient.version)
                }
            }
    }

    @Test
    fun `find all launchers with not current maintenance`() {
        Mockito.doReturn(listOf(LAUNCHER_MAC, LAUNCHER_WIN, LAUNCHER_LINUX)).`when`(launcherService).findAll()
        maintenanceService.setUp(MaintenanceService.Type.GAME)
        webClient.spec(ClientApi.clientVersions()).exchange().expectStatus().isOk
        maintenanceService.setDown(MaintenanceService.Type.GAME)
        maintenanceService.setUp(MaintenanceService.Type.MANUAL)
        webClient.spec(ClientApi.clientVersions()).exchange().expectStatus().isOk
        maintenanceService.setDown(MaintenanceService.Type.MANUAL)
    }

    @Test
    fun `find all launchers in maintenance`() {
        maintenanceService.setUp(MaintenanceService.Type.LAUNCHER)
        Mockito.doReturn(listOf(LAUNCHER_MAC, LAUNCHER_WIN, LAUNCHER_LINUX)).`when`(launcherService).findAll()
        maintenanceService.setDown(MaintenanceService.Type.LAUNCHER)
        maintenanceService.setUp(MaintenanceService.Type.ALL)
        Mockito.doReturn(listOf(LAUNCHER_MAC, LAUNCHER_WIN, LAUNCHER_LINUX)).`when`(launcherService).findAll()
        maintenanceService.setDown(MaintenanceService.Type.ALL)
    }

    companion object {
        private val LAUNCHER_WIN = Launcher(1, SystemType.WINDOWS, "1.0.0", byteArrayOf(1, 3, 4), "winLauncher", 100)
        private val LAUNCHER_MAC = Launcher(2, SystemType.MACOS, "1.2.0", byteArrayOf(1, 2, 4), "macLauncher", 1001)
        private val LAUNCHER_LINUX = Launcher(2, SystemType.LINUX, "1.3.0", byteArrayOf(2, 2, 4), "linuxLauncher", 1021)
        private val BUILD_1 = Build(1, "test1", "testBranch1", true)
        private val BUILD_2 = Build(2, "test2", "testBranch2", false)
        private val MAIN_CATEGORY_BUILD_1 = Category(
            id = 1,
            type = ContentType.MAIN,
            required = true,
            build = BUILD_1
        ).also { category ->
            category.items.add(
                Item(
                    id = 1,
                    name = "firstItem",
                    category = category
                ).also { item ->
                    item.files.add(
                        File(
                            id = 1,
                            path = "testPath",
                            lastChangeDate = LocalDateTime.now(),
                            item = item,
                            size = 100,
                            md5 = byteArrayOf(1, 2, 3, 5)
                        ),
                    )
                    item.files.add(
                        File(
                            id = 2,
                            path = "testPath2",
                            lastChangeDate = LocalDateTime.now().minusDays(2),
                            item = item,
                            size = 101,
                            md5 = byteArrayOf(1, 2, 3, 2)
                        )
                    )
                }
            )
        }
        private val EXTRA_CATEGORY_BUILD_1 = Category(
            id = 2,
            type = ContentType.EXTRA,
            required = false,
            build = BUILD_1
        ).also { category ->
            category.items.add(
                Item(
                    id = 2,
                    name = "secondItem",
                    category = category
                ).also { item ->
                    item.files.add(
                        File(
                            id = 3,
                            path = "testPath3",
                            lastChangeDate = LocalDateTime.now(),
                            item = item,
                            size = 1000,
                            md5 = byteArrayOf(3, 2, 5, 1)
                        )
                    )
                    item.files.add(
                        File(
                            id = 4,
                            path = "testPath4",
                            lastChangeDate = LocalDateTime.now().minusDays(2),
                            item = item,
                            size = 1001,
                            md5 = byteArrayOf(3, 2, 5, 2)
                        )
                    )
                }
            )
        }
    }
}