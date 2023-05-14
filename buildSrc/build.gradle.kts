plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("common-plugin") {
            description = "Common dependency plugin"
            displayName = "Common dependencies"
            id = "com.lezenford.mfr.plugin.common"
            implementationClass = "CommonProjectPlugin"
        }
    }
}