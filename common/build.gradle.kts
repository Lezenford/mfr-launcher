import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version kotlinVersion
}

dependencies {
    // spring boot
    compileOnly("org.springframework.boot:spring-boot-starter:$springBootVersion")

    // jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    // netty
    compileOnly("io.netty:netty-all:$nettyVersion")

    // apache common
    api("commons-io:commons-io:$apacheCommonVersion")

    // logging
    api("org.apache.logging.log4j:log4j-api:2.14.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = jvmVersion
    }
}