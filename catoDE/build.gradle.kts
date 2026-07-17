plugins {
    kotlin("jvm") version "2.2.20"
    // Since Kotlin 2.0 the Compose compiler is a separate Kotlin plugin,
    // versioned in lockstep with Kotlin itself.
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    // Compose Multiplatform 1.8.x+ is the K2 line that pairs with Kotlin 2.1+.
    id("org.jetbrains.compose") version "1.8.2"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(project(":"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "com.catode.MainKt"
    }
}
