import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `common-dependencies`
    id("org.openjfx.javafxplugin") version openfxPluginVersion
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

version = "3.1.4"

dependencies {
    //	spring-boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-rsocket")
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

    // netty
    implementation("io.netty:netty-all:$nettyVersion")

    //javafx system tray
    implementation("com.dustinredmond.fxtrayicon:FXTrayIcon:3.0.0")

    //  modules
    implementation(project(":common"))
    implementation(project(":desktop:javafx"))
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
   // workingDir = File("/Users/av-plekhanov/Library/Application Support/CrossOver/Bottles/The Elder Scrolls III Morrowind/drive_c/Games/M[FR]")
    workingDir = File("D:/Games/M[FR]")
}

tasks.jar {
    manifest {
        attributes["Implementation-Title"] = "Morrowind Fullrest Repack Launcher"
        attributes["Implementation-Version"] = archiveVersion
    }
}