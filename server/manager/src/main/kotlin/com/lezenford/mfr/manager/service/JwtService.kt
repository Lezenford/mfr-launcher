package com.lezenford.mfr.manager.service

import com.lezenford.mfr.manager.configuration.JWT_CACHE
import io.jsonwebtoken.Jwts
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*

@Service
class JwtService {
    private val key: PrivateKey by lazy {
        val pem = this.javaClass.classLoader.getResourceAsStream("keys/private.pem")
            ?.readAllBytes()
            ?.decodeToString()
            ?.replace("\n", "")
            ?.replace("\r", "")
            ?.removeSurrounding("-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----")
            ?: throw IllegalArgumentException("Private key is not found")

        KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)))
    }

    @Cacheable(value = [JWT_CACHE], key = "'generateToken'")
    suspend fun generateToken(): String {
        return Jwts.builder()
            .setIssuedAt(Date())
            .setExpiration(Date(Date().time + 3600000L))
            .signWith(key)
            .compact()
    }
}