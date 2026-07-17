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
    // The catoscript library (Parser/Interpreter) currently lives in the
    // root project. Repoint this at ":cato-kotlin" if/when the lib is
    // extracted into its own module.
    implementation(project(":"))
}

compose.desktop {
    application {
        mainClass = "com.catode.MainKt"
    }
}
