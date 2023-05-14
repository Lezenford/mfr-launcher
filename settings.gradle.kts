rootProject.name = "mfr-launcher"
include(
    "desktop:application",
    "desktop:javafx",
    "desktop:updater",
   "desktop:configurator",
    "server:storage",
    "server:manager",
    "common"
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

pluginManagement {
    plugins {
        id("java")
        id("idea")
        id("application")
        kotlin("jvm")
        kotlin("plugin.spring")
        kotlin("plugin.jpa")
        kotlin("kapt")
        id("io.spring.dependency-management")
        id("org.springframework.boot")
        id("org.openjfx.javafxplugin")
    }
}