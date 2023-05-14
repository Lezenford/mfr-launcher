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

version = "3.0.7"

application {
    mainClass.set("com.lezenford.mfr.server.StorageServerApplicationKt")
}

dependencies {
    // spring-boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-rsocket")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    implementation("org.springdoc:springdoc-openapi-webflux-ui:$springDocsVersion")

    // micrometer
    implementation("io.micrometer:micrometer-registry-prometheus:1.9.2")

    // database
    implementation("org.liquibase:liquibase-core:$liquibaseVersion")
    implementation("com.h2database:h2")
    runtimeOnly("mysql:mysql-connector-java")

    // netty
    implementation("io.netty:netty-all:$nettyVersion")

    // cache
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    // jwt
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:$jgitVersion")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:$jgitVersion")

    // test
    testImplementation("org.awaitility:awaitility:$awaitilityVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")

    // common objects
    implementation(project(":common"))
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "Morrowind Fullrest Repack Storage Server"
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
        val public = Paths.get("$buildDir/resources/test/key/public.pem")
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
        val private = Paths.get("$buildDir/resources/test/key/private.pem")
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