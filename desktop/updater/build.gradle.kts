import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    kotlin("jvm") version kotlinVersion
}

version = "0.1-SNAPSHOT"

application {
    mainClass.set("ru.fullrest.mfr.updater.PluginConfigurationUtilityUpdaterKt")
}

dependencies {
    // log4j2
    implementation("org.apache.logging.log4j:log4j-api:2.13.3")
    implementation("org.apache.logging.log4j:log4j-core:2.13.3")

    //  kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

    //  apache-commons
    implementation("commons-io:commons-io:$apacheCommonVersion")

    //  modules
    implementation(project(":common"))
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

    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}
