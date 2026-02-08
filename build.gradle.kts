plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.emilburzo.xcontest2db"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

val kotlin_exposed_version = "0.56.0"
val ktor_version = "3.0.1"
dependencies {
    testImplementation(kotlin("test-junit"))
    implementation("io.ktor:ktor-client-cio:${ktor_version}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktor_version}")
    implementation("io.ktor:ktor-serialization-jackson:${ktor_version}")
    implementation("org.jetbrains.exposed:exposed-core:${kotlin_exposed_version}")
    implementation("org.jetbrains.exposed:exposed-dao:${kotlin_exposed_version}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${kotlin_exposed_version}")
    implementation("org.jetbrains.exposed:exposed-jodatime:${kotlin_exposed_version}")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("org.jsoup:jsoup:1.18.1")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("ch.qos.logback:logback-core:1.5.12")
    implementation("net.postgis:postgis-jdbc:2.5.1")
}

tasks.test {
    useJUnit()
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("xcontest2db")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "com.emilburzo.main.MainKt"))
        }
    }
}

application {
    mainClass.set("com.emilburzo.main.MainKt")
}
