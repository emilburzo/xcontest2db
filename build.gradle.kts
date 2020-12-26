import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.emilburzo.xcontest2db"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

val kotlin_exposed_version = "0.28.1"
val ktor_version = "1.5.0"
dependencies {
    testImplementation(kotlin("test-junit"))
    implementation("io.ktor:ktor-jackson:${ktor_version}")
    implementation("io.ktor:ktor-client-core:${ktor_version}")
    implementation("io.ktor:ktor-client-cio:${ktor_version}")
    implementation("org.jetbrains.exposed:exposed-core:${kotlin_exposed_version}")
    implementation("org.jetbrains.exposed:exposed-dao:${kotlin_exposed_version}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${kotlin_exposed_version}")
    implementation("org.jetbrains.exposed:exposed-jodatime:${kotlin_exposed_version}")
    implementation("com.rometools:rome:1.12.2")
    implementation("org.postgresql:postgresql:42.2.8")
    implementation("org.jsoup:jsoup:1.12.1")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("ch.qos.logback:logback-core:1.2.3")
    implementation("net.postgis:postgis-jdbc:2.5.0")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("xcontest2db")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "MainKt"))
        }
    }
}

application {
    mainClassName = "MainKt"
}