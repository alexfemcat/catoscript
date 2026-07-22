plugins {
    kotlin("jvm") version "2.2.20"
    `maven-publish`
}

group = "com.catoscript"
version = providers.gradleProperty("catoscript-web.version").get()

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.catoscript"
            artifactId = "catoscript-web"
            version = project.version.toString()
        }
    }
    repositories {
        mavenLocal()
    }
}
