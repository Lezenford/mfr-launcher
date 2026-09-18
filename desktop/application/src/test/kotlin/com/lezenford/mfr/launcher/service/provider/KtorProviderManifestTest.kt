package com.lezenford.mfr.launcher.service.provider

import com.lezenford.mfr.launcher.testProperties
import com.lezenford.mfr.schema.v1.Schema
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.zip.GZIPOutputStream

class KtorProviderManifestTest {

    @TempDir
    lateinit var folder: Path

    private val schema: ByteArray = Schema.newBuilder().setVersion("5.0.15").build().toByteArray()
    private val packed: ByteArray = ByteArrayOutputStream().also { bytes ->
        GZIPOutputStream(bytes).use { it.write(schema) }
    }.toByteArray()

    private fun provider(handler: (String) -> Pair<HttpStatusCode, ByteArray>, requested: MutableList<String>): KtorProvider {
        val client = HttpClient(MockEngine { request ->
            val path = request.url.encodedPath
            requested += path
            val (status, body) = handler(path)
            respond(content = ByteReadChannel(body), status = status)
        }) { install(HttpTimeout) }
        return KtorProvider(client, testProperties(folder))
    }

    @Test
    fun `манифест читается из сжатой копии, сырой не запрашивается`() = runBlocking {
        val requested = mutableListOf<String>()
        val provider = provider({ path -> HttpStatusCode.OK to (if (path == "/packed") packed else schema) }, requested)

        val result = provider.findGameSchema("https://host.example", "raw", "packed")

        assertEquals("5.0.15", result.version)
        assertEquals(listOf("/packed"), requested)
    }

    @Test
    fun `копии манифеста нет — читается сырой`() = runBlocking {
        val requested = mutableListOf<String>()
        val provider = provider({ path -> if (path == "/packed") HttpStatusCode.NotFound to ByteArray(0) else HttpStatusCode.OK to schema }, requested)

        assertEquals("5.0.15", provider.findGameSchema("https://host.example", "raw", "packed").version)
        assertEquals(listOf("/packed", "/raw"), requested)
    }

    @Test
    fun `копия манифеста не читается как gzip — читается сырой`() = runBlocking {
        val requested = mutableListOf<String>()
        val provider = provider({ path -> HttpStatusCode.OK to (if (path == "/packed") ByteArray(100) { 1 } else schema) }, requested)

        assertEquals("5.0.15", provider.findGameSchema("https://host.example", "raw", "packed").version)
        assertEquals(listOf("/packed", "/raw"), requested)
    }

    @Test
    fun `без предложенной копии манифест читается сырым, как раньше`() = runBlocking {
        val requested = mutableListOf<String>()
        val provider = provider({ HttpStatusCode.OK to schema }, requested)

        assertEquals("5.0.15", provider.findGameSchema("https://host.example", "raw").version)
        assertEquals(listOf("/raw"), requested)
    }
}
