import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `common-dependencies`
    id("org.openjfx.javafxplugin") version openfxPluginVersion
    id("com.google.protobuf") version "0.9.4"
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

version = "3.2.0"
// Configure Protobuf plugin
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    // generateProtoTasks {
    //     all().forEach { task ->
    //         task.builtins {
    //             id("kotlin")
    //         }
    //     }
    // }
}
dependencies {
    //	spring-boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
//    kapt("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("org.junit.vintage:junit-vintage-engine")
    }

    //Javafx
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:$kotlinCoroutinesVersion")

    //	database
    runtimeOnly("com.h2database:h2:1.4.200")
    implementation("org.liquibase:liquibase-core:$liquibaseVersion")
    implementation("org.liquibase.ext:liquibase-nochangeloglock:1.1")

    // cache
    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    //javafx system tray
    implementation("com.dustinredmond.fxtrayicon:FXTrayIcon:3.0.0")

    implementation("io.ktor:ktor-client-core:2.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:2.1.3")
    implementation("io.ktor:ktor-client-logging:2.1.3")

    implementation("io.ktor:ktor-client-java:2.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.3")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation("com.google.protobuf:protoc:3.25.5")
    implementation("com.google.protobuf:protobuf-kotlin:3.25.5")

    //  modules
    implementation(project(":common"))
    implementation(project(":desktop:javafx"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = jvmVersion
        freeCompilerArgs = freeCompilerArgs + "-Xdebug"
    }
}



tasks.bootRun {
    doFirst {
        jvmArgs = listOf(
            // "-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000",
            "-Djava.awt.headless=false",
            "-Dhttps.protocols=TLSv1,TLSv1.1,TLSv1.2",
            "-Djdk.tls.client.protocols=TLSv1.2",
            "-Djdk.tls.acknowledgeCloseNotify=true"
        )
    }
    workingDir = File("/Users/av-plekhanov/MFR")
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "Morrowind Fullrest Repack Launcher"
        attributes["Implementation-Version"] = archiveVersion
    }
}