package com.lezenford.mfr.server

import org.junit.jupiter.api.AfterAll
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import java.nio.file.Files
import java.nio.file.Path

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@Sql(scripts = ["/cleanDb.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseTest {
    companion object {
        private const val PREFIX = "test_mfr-launcher_"
        private val TEST_TEMP_DIR = Files.createTempDirectory(PREFIX)
        fun createTempDirectory(parent: Path? = null): Path {
            Files.createDirectories(TEST_TEMP_DIR)
            return Files.createTempDirectory(parent ?: TEST_TEMP_DIR, PREFIX)
        }

        fun createTempFile(parent: Path? = null): Path {
            Files.createDirectories(TEST_TEMP_DIR)
            return Files.createTempFile(parent ?: TEST_TEMP_DIR, PREFIX, "")
        }

        @JvmStatic
        @AfterAll
        fun removeTempFiles() {
            TEST_TEMP_DIR.toFile().deleteRecursively()
        }
    }
}