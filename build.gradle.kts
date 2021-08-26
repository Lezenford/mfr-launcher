plugins {
    id("java")
    id("idea")
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
    gradleVersion = "6.8.2"
}
group = "ru.fullrest.mfr"

allprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
}