import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyPairGenerator
import java.util.*

plugins {
    id("java")
    id("application")
    `common-dependencies`
}

version = "3.0.0"

application {
    mainClass.set("com.lezenford.mfr.manager.ManagerServerApplicationKt")
}

allOpen {
    annotation("org.springframework.data.relational.core.mapping.Table")
}

dependencies {
    // spring-boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    // implementation("org.springframework:spring-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    // database
    implementation("org.liquibase:liquibase-core:$liquibaseVersion")
    implementation("com.h2database:h2")
    implementation("io.r2dbc:r2dbc-h2")

    // cache
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    // telegramBot
    implementation("org.telegram:telegrambots:$telegramBotVersion")
    implementation("org.telegram:telegrambotsextensions:$telegramBotVersion")

    // jwt
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
    implementation("io.netty:netty-resolver-dns-native-macos:$nettyVersion:osx-aarch_64")

    // test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    testImplementation("com.squareup.okhttp3:okhttp:4.9.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("io.projectreactor:reactor-test")
    kaptTest("org.springframework.boot:spring-boot-configuration-processor")

    // common objects
    implementation(project(":common"))
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "Morrowind Fullrest Repack Manager Server"
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = application.mainClass
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = jvmVersion
    }
}

tasks.withType<Test> {
    doFirst {
        val keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair()
        val public = Paths.get("$buildDir/resources/test/keys/public.pem")
        mkdir(public.parent)
        Files.deleteIfExists(public)
        FileWriter(public.toFile()).use {
            it.write(
                "-----BEGIN PUBLIC KEY-----${
                    Base64.getEncoder().encode(keyPair.public.encoded)
                        .decodeToString()
                }-----END PUBLIC KEY-----"
            )
        }
        val private = Paths.get("$buildDir/resources/test/keys/private.pem")
        mkdir(private.parent)
        Files.deleteIfExists(private)
        FileWriter(private.toFile()).use {
            it.write(
                "-----BEGIN RSA PRIVATE KEY-----${
                    Base64.getEncoder().encode(keyPair.private.encoded)
                        .decodeToString()
                }-----END RSA PRIVATE KEY-----"
            )
        }
    }
    useJUnitPlatform()
}