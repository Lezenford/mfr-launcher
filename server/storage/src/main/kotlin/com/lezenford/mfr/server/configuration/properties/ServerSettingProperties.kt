package com.lezenford.mfr.server.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.core.io.ClassPathResource
import java.nio.file.Path
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

@ConstructorBinding
@ConfigurationProperties(prefix = "setting")
data class ServerSettingProperties(
    val launcherFolder: String,
    val netty: Netty,
    val build: Git,
    val manual: Git,
    val web: Web
) {
    data class Netty(
        val port: Int,
        val timeout: Long = 300,
        val security: Security? = null
    ) {
        data class Security(
            val cert: Path,
            val key: Path
        )
    }

    data class Git(
        val local: String,
        val remote: String,
        val key: String
    )

    data class Web(
        val security: Security
    ) {
        data class Security(
            val keyFile: String
        ) {
            val key: PublicKey = kotlin.run {
                val keyString = ClassPathResource(keyFile).inputStream.readAllBytes().decodeToString()
                    .replace("\n", "")
                    .replace("\r", "")
                    .removeSurrounding("-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----")
                val keyFactory = KeyFactory.getInstance("RSA")
                keyFactory.generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(keyString)))
            }
        }
    }
}