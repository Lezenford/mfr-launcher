import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

val PluginDependenciesSpec.`common-dependencies`: PluginDependencySpec
    get() = id("com.lezenford.mfr.plugin.common")

class CommonProjectPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply {
            apply("application")
            apply("org.jetbrains.kotlin.jvm")
            apply("org.jetbrains.kotlin.plugin.spring")
            apply("org.jetbrains.kotlin.plugin.jpa")
            apply("org.jetbrains.kotlin.kapt")

            apply("io.spring.dependency-management")
            apply("org.springframework.boot")
        }

        //kotlin
        target.dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-reflect")
        target.dependencies.add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        target.dependencies.add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
        target.dependencies.add("implementation", "io.projectreactor.kotlin:reactor-kotlin-extensions")

        //jackson
        target.dependencies.add("implementation", "com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
        target.dependencies.add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
        target.dependencies.add(
            "implementation",
            "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion"
        )

        // apache common
        target.dependencies.add("implementation", "commons-io:commons-io:$apacheCommonVersion")

        // logging
        target.dependencies.add("implementation", "org.apache.logging.log4j:log4j-api:$log4jVersion")
    }
}