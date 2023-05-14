import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.openjfx.javafxplugin")
    `common-dependencies`
}

val jar: Jar by tasks
val bootJar: BootJar by tasks

bootJar.enabled = false
jar.enabled = true

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.fxml")
    configuration = "compileOnly"
}

dependencies {
    api("org.springframework.boot:spring-boot-starter")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:$kotlinCoroutinesVersion")
    compileOnly(project(":common"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = jvmVersion
    }
}