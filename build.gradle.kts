import org.gradle.api.tasks.application.CreateStartScripts

plugins {
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    `java-library`
    `maven-publish`
    id("application")
    id("com.gradleup.shadow") version "9.0.0-beta12"
    // NOTE: the Compose Multiplatform plugin is intentionally NOT applied here.
    // This root project is the catoscript CLI library (JVM only). Applying
    // org.jetbrains.compose here pulls in the Kotlin/Native target machinery
    // and triggers the KonanTarget$IOS_ARM32 init crash. Compose lives only
    // in the :catoDE module.
}

group = "com.catoscript"
version = providers.gradleProperty("catoscript.version").get()

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.catoscript.cli.RunScriptKt")
}

// The shadow plugin auto-creates a second distribution called "shadow"
// alongside the application plugin's "main" distribution. The shadow
// distribution ships only the fat jar in lib/ plus Gradle's standard
// launcher scripts in bin/ (which auto-discover the fat jar and run it).
// The application plugin's "main" distribution expands every runtime
// dep into its own lib/*.jar — wrong shape for a fat-jar install.
//
// We disable the regular jar, the regular startScripts, and the main
// distribution's tasks, and use "shadow" as the user-facing distribution:
//   ./gradlew shadowJar         -> build/libs/catoscript-<version>.jar
//   ./gradlew shadowDistZip     -> build/distributions/catoscript-shadow-*.zip
//   ./gradlew installShadowDist -> build/install/catoscript-shadow/
tasks.jar {
    enabled = false
}
tasks.named("startScripts") {
    enabled = false
}
listOf("distZip", "distTar", "installDist", "assembleDist").forEach { taskName ->
    tasks.named(taskName) { enabled = false }
}

// Rename the launchers from "catoscript" to "cato". The shadow plugin's
// startShadowScripts extends the application's CreateStartScripts, so
// applicationName is the same property the regular startScripts use.
tasks.named<CreateStartScripts>("startShadowScripts") {
    applicationName = "cato"
}

// shadowJar is the user-facing jar: no classifier suffix, Main-Class
// manifest, service files merged. Publishes to mavenLocal as the only
// artifact (see publishing block below).
tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "com.catoscript.cli.RunScriptKt"
    }
}

// Mark the bash launcher executable in the installed directory. Windows
// ignores POSIX bits.
tasks.named("installShadowDist") {
    doLast {
        val catoScript = layout.buildDirectory
            .dir("install/catoscript-shadow/bin/cato")
            .get()
            .asFile
        if (catoScript.exists()) {
            catoScript.setExecutable(true, false)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["shadow"])
            groupId = "com.catoscript"
            artifactId = "catoscript"
            version = project.version.toString()
        }
    }
    repositories {
        mavenLocal()
    }
}
