import org.gradle.api.tasks.application.CreateStartScripts

plugins {
    kotlin("jvm") version "2.2.20"
    id("org.jetbrains.compose") version "1.11.1"
    `application`
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

group = "com.catode"
version = "0.1.0-LOCAL"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.catoscript:catoscript:0.3.0-LOCAL")
}

application {
    mainClass.set("com.catode.MainKt")
}

// catoDE ships as a single fat jar. The application plugin's main
// distribution expands every runtime dep into its own lib/*.jar,
// which is the wrong shape for a double-click install. Use the
// shadow plugin's "shadow" distribution instead: one jar, manifest
// points at our Main-Class, service files merged.
tasks.jar {
    enabled = false
}
tasks.named("startScripts") {
    enabled = false
}
listOf("distZip", "distTar", "installDist", "assembleDist").forEach { taskName ->
    tasks.named(taskName) { enabled = false }
}

tasks.named<CreateStartScripts>("startShadowScripts") {
    applicationName = "catoDE"
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "com.catode.MainKt"
    }
}
