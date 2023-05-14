plugins {
    id("java")
    id("idea")
    id("application")
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.jpa") version kotlinVersion apply false
    kotlin("kapt") version kotlinVersion apply false
    id("io.spring.dependency-management") version springBootDependencyManagementVersion apply false
    id("org.springframework.boot") version springBootVersion apply false
    id("org.openjfx.javafxplugin") version openfxPluginVersion apply false
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

tasks.withType<Wrapper> {
    gradleVersion = gradleWrapperVersion
}
group = "ru.fullrest.mfr"
