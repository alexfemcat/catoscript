plugins {
    kotlin("jvm") version "2.2.20"
    `java-library`
    `maven-publish`
    id("application")
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

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.catoscript"
            artifactId = "catoscript"
            version = project.version.toString()
        }
    }
    repositories {
        mavenLocal()
    }
}