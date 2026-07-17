rootProject.name = "catoscript"

include(":catoDE")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal() // Added for plugin resolution
    }
}
