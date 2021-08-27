import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion
    kotlin("plugin.jpa") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("org.openjfx.javafxplugin") version openfxPluginVersion
    id("io.spring.dependency-management") version springBootDependencyManagementVersion
    id("org.springframework.boot") version springBootVersion
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

version = "3.0.0"

dependencies {
    //	spring-boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-rest")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("org.junit.vintage:junit-vintage-engine")
    }

    //  kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:$kotlinCoroutinesVersion")

    //	database
    runtimeOnly("com.h2database:h2")
    implementation("org.liquibase:liquibase-core:$liquibaseVersion")

    //  apache-commons
    implementation("commons-io:commons-io:$apacheCommonVersion")

    // cache
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    //  jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    // netty
    implementation("io.netty:netty-all:$nettyVersion")

    //javafx system tray
    implementation("com.dustinredmond.fxtrayicon:FXTrayIcon:3.0.0")

    //  modules
    implementation(project(":common"))
    api(project(":desktop:javafx"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = jvmVersion
    }
}

tasks.bootRun {
    doFirst {
        jvmArgs = listOf(
//            "-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000",
            "-Djava.awt.headless=false",
            "-Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2"
        )
    }
    workingDir = File("C:/Games/M[FR]")
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "Morrowind Fullrest Repack Launcher"
        attributes["Implementation-Version"] = archiveVersion
    }
}