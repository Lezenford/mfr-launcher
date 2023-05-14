import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    `common-dependencies`
}

version = "0.1-SNAPSHOT"

application {
    mainClass.set("ru.fullrest.mfr.updater.PluginConfigurationUtilityUpdaterKt")
}

dependencies {
    // log4j2
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")

    //  modules
    implementation(project(":common"))

    // netty
    implementation("io.netty:netty-all:$nettyVersion")

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    compileOnly("org.springframework.boot:spring-boot-starter:$springBootVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = jvmVersion
    }
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "Morrowind Fullrest Repack Plugins Configuration Utility Updater"
        attributes["Implementation-Version"] = archiveVersion
        attributes["Main-Class"] = "ru.fullrest.mfr.updater.PluginConfigurationUtilityUpdaterKt"
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}
