package com.lezenford.mfr.server.controller.rest

import com.lezenford.mfr.common.extensions.spec
import com.lezenford.mfr.common.protocol.http.rest.ServiceApi
import com.lezenford.mfr.server.BaseTest
import io.jsonwebtoken.Jwts
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

internal class SwaggerAuthControllerTest : BaseTest() {

    private val key: PrivateKey by lazy {
        val pem = this.javaClass.classLoader.getResourceAsStream("key/private.pem")
            ?.readAllBytes()
            ?.decodeToString()
            ?.replace("\n", "")
            ?.removeSurrounding("-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----")
            ?: throw IllegalArgumentException("Private key is not found")

        KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)))
    }

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun `successfully auth with jwt`() {
        val jwt = Jwts.builder()
            .setIssuedAt(Date())
            .setExpiration(Date(Date().time + 3600000L))
            .signWith(key)
            .compact()
        webClient.get().uri("/swagger?key=$jwt").exchange().expectStatus().is3xxRedirection
    }

    @Test
    fun `incorrect auth with jwt`() {
        val jwt = Jwts.builder()
            .setIssuedAt(Date())
            .setExpiration(Date(Date().time + 3600000L))
            .signWith(key)
            .compact()
        webClient.get().uri("/swagger?key=${jwt}123").exchange().expectStatus().isNotFound
    }

    @Test
    fun `successfully auth filter with jwt by cookie header`() {
        val jwt = Jwts.builder()
            .setIssuedAt(Date())
            .setExpiration(Date(Date().time + 3600000L))
            .signWith(key)
            .compact()
        webClient.spec(ServiceApi.builds()).cookie(com.lezenford.mfr.common.SESSION_ID, jwt).exchange()
            .expectStatus().is2xxSuccessful
    }

    @Test
    fun `successfully auth filter with jwt by authorization header`() {
        val jwt = Jwts.builder()
            .setIssuedAt(Date())
            .setExpiration(Date(Date().time + 3600000L))
            .signWith(key)
            .compact()
        webClient.spec(ServiceApi.builds()).header(HttpHeaders.AUTHORIZATION, "Bearer $jwt").exchange()
            .expectStatus().is2xxSuccessful
    }
}