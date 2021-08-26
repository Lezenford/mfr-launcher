rootProject.name = "mfr-launcher"
include(
    "desktop:application",
    "desktop:javafx",
    "desktop:updater",
    "desktop:configurator",
    "server",
    "common"
)

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        jcenter()
    }
}