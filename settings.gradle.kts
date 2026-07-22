rootProject.name = "catoscript"

include(":catoDE")
include(":catoscript-libs:catoscript-web")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal() // Added for plugin resolution
    }
}
