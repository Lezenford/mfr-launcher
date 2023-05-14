import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `common-dependencies`
    id("org.openjfx.javafxplugin") version openfxPluginVersion
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    runtimeOnly("org.springframework.boot:spring-boot-devtools")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude("org.junit.vintage:junit-vintage-engine")
    }
    //  modules
    implementation(project(":common"))
    implementation(project(":desktop:javafx"))
}

tasks.bootRun {
    doFirst {
        jvmArgs = listOf(
//            "-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000",
            "-Djava.awt.headless=false"
        )
    }
    workingDir = File("/Users/av-plekhanov/MFR/morrowind-fullrest-repack")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = jvmVersion
    }
}
