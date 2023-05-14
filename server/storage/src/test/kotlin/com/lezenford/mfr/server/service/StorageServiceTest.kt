package com.lezenford.mfr.server.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.lezenford.mfr.server.BaseTest
import com.lezenford.mfr.server.model.entity.Build
import com.lezenford.mfr.server.model.repository.BuildRepository
import com.lezenford.mfr.server.service.model.CategoryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import com.lezenford.mfr.common.protocol.enums.ContentType
import com.lezenford.mfr.common.protocol.file.CONTENT_FILE_NAME
import com.lezenford.mfr.common.protocol.file.Content
import java.nio.file.Files
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.writeBytes
import kotlin.random.Random

@TestPropertySource(properties = ["setting.build.local=\${StorageServiceTest.fileFolder}"])
internal class StorageServiceTest : BaseTest() {

    @Autowired
    private lateinit var storageService: StorageService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var buildRepository: BuildRepository

    @Autowired
    protected lateinit var categoryService: CategoryService

    @Test
    fun `create new build info`() {
        val content = content
        CONTENT_FILE.writeBytes(objectMapper.writeValueAsBytes(content))
        val build = Build(name = "test", branch = TEMP_FOLDER.name)
        buildRepository.save(build)
        storageService.updateBuild(build)

        val categories = categoryService.findAllByBuildId(buildId = build.id)
        assertThat(categories).hasSize(3)
        categories.forEach { savedCategory ->
            val sourceCategory = content.categories.first { it.type == savedCategory.type }
            assertThat(savedCategory.required).isEqualTo(sourceCategory.required)
            assertThat(savedCategory.items).hasSameSizeAs(sourceCategory.items)

            savedCategory.items.forEach { savedItem ->
                val sourceItem = sourceCategory.items.first { it.name == savedItem.name }
                assertThat(savedItem.files).hasSameSizeAs(sourceItem.files)
                savedItem.files.forEach { savedFile ->
                    sourceItem.files.first { it.path == savedFile.path }
                }
            }
        }
    }

    @Test
    fun `add files to existing build`() {
        var content = content
        CONTENT_FILE.writeBytes(objectMapper.writeValueAsBytes(content))
        val build = Build(name = "test", branch = TEMP_FOLDER.name)
        buildRepository.save(build)
        storageService.updateBuild(build)

        content = content.copy(
            categories = content.categories.map { category ->
                category.copy(
                    items = category.items.map { item ->
                        item.copy(
                            files = item.files + Content.Category.Item.File(createTempFile(TEMP_FOLDER).also {
                                it.writeBytes(Random.nextBytes(1024 * 1024))
                            }.name)
                        )
                    }
                )
            }
        )
        CONTENT_FILE.writeBytes(objectMapper.writeValueAsBytes(content))
        storageService.updateBuild(build)

        val categories = categoryService.findAllByBuildId(buildId = build.id)
        assertThat(categories).hasSize(3)
        categories.forEach { savedCategory ->
            val sourceCategory = content.categories.first { it.type == savedCategory.type }
            assertThat(savedCategory.required).isEqualTo(sourceCategory.required)
            assertThat(savedCategory.items).hasSameSizeAs(sourceCategory.items)

            savedCategory.items.forEach { savedItem ->
                val sourceItem = sourceCategory.items.first { it.name == savedItem.name }
                assertThat(savedItem.files).hasSameSizeAs(sourceItem.files)
                savedItem.files.forEach { savedFile ->
                    sourceItem.files.first { it.path == savedFile.path }
                }
            }
        }
    }

    @Test
    fun `remove files from existing build`() {
        var content = content.copy(
            categories = content.categories.map { category ->
                category.copy(
                    items = category.items.map { item ->
                        item.copy(
                            files = item.files + Content.Category.Item.File(createTempFile(TEMP_FOLDER).also {
                                it.writeBytes(Random.nextBytes(1024 * 1024))
                            }.name)
                        )
                    }
                )
            }
        )
        CONTENT_FILE.writeBytes(objectMapper.writeValueAsBytes(content))
        val build = Build(name = "test", branch = TEMP_FOLDER.name)
        buildRepository.save(build)
        storageService.updateBuild(build)

        content = content.copy(
            categories = content.categories.map { category ->
                category.copy(
                    items = category.items.map { item ->
                        item.copy(
                            files = listOf(item.files.first())
                        )
                    }
                )
            }
        )
        CONTENT_FILE.writeBytes(objectMapper.writeValueAsBytes(content))
        storageService.updateBuild(build)

        val categories = categoryService.findAllByBuildId(buildId = build.id)
        assertThat(categories).hasSize(3)
        categories.forEach { savedCategory ->
            val sourceCategory = content.categories.first { it.type == savedCategory.type }
            assertThat(savedCategory.required).isEqualTo(sourceCategory.required)
            assertThat(savedCategory.items).hasSameSizeAs(sourceCategory.items)

            savedCategory.items.forEach { savedItem ->
                val sourceItem = sourceCategory.items.first { it.name == savedItem.name }
                assertThat(savedItem.files).hasSizeGreaterThan(sourceItem.files.size)
                savedItem.files.forEach { savedFile ->
                    sourceItem.files.find { it.path == savedFile.path }?.also {
                        assertThat(savedFile.active).isTrue
                    } ?: assertThat(savedFile.active).isFalse
                }
            }
        }
    }

    companion object {
        private val TEMP_FOLDER = createTempDirectory()
        private val CONTENT_FILE = Files.createFile(TEMP_FOLDER.resolve(CONTENT_FILE_NAME))

        private val content
            get() = Content(
                categories = listOf(
                    Content.Category(
                        type = ContentType.MAIN,
                        required = true,
                        items = listOf(
                            Content.Category.Item(
                                name = "MainContentItem",
                                files = listOf(
                                    Content.Category.Item.File(
                                        path = createTempFile(TEMP_FOLDER).also {
                                            it.writeBytes(Random.nextBytes(1024 * 1024))
                                        }.name
                                    )
                                )
                            )
                        )
                    ),
                    Content.Category(
                        type = ContentType.EXTRA,
                        required = false,
                        items = listOf(
                            Content.Category.Item(
                                name = "ExtraContentItem",
                                files = listOf(
                                    Content.Category.Item.File(
                                        path = createTempFile(TEMP_FOLDER).also {
                                            it.writeBytes(Random.nextBytes(1024 * 1024))
                                        }.name
                                    )
                                )
                            )
                        )
                    ),
                    Content.Category(
                        type = ContentType.OPTIONAL,
                        required = false,
                        items = listOf(
                            Content.Category.Item(
                                name = "OptionalContentItem",
                                files = listOf(
                                    Content.Category.Item.File(
                                        path = createTempFile(TEMP_FOLDER).also {
                                            it.writeBytes(Random.nextBytes(1024 * 1024))
                                        }.name
                                    )
                                )
                            )
                        )
                    ),
                )
            )

        @BeforeAll
        @JvmStatic
        fun prepareProperties() {
            System.setProperty("StorageServiceTest.fileFolder", TEMP_FOLDER.parent.absolutePathString())
        }
    }
}