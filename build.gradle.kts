import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    application
    kotlin("plugin.serialization") version "1.4.10"
}
group = "net.willemml"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/kotlin/kotlinx")
    maven("https://maven.daporkchop.net")
    maven("https://repo.opencollab.dev/maven-releases/")
}
dependencies {
    implementation("com.github.willemml:kt-cmd:v1.6.2")
    implementation("org.slf4j:slf4j-nop:2.0.5")
    implementation("io.ktor:ktor-jackson:1.6.8")
    implementation("io.ktor:ktor-network:1.4.0")
    implementation("io.ktor:ktor-server-jetty:1.4.0")
    implementation("io.ktor:ktor-html-builder:1.6.8")
    implementation("io.ktor:ktor-client-apache:1.6.8")
    implementation("io.ktor:ktor-client-jackson:1.4.0")
    implementation("io.ktor:ktor-client-logging:1.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")
    implementation("com.github.Steveice10:MCProtocolLib:1.19.2-1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")
    implementation("net.daporkchop.lib:minecraft-text:0.5.5-20201106.114836-37")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}

val run: JavaExec by tasks.getting(JavaExec::class) {
    standardInput = System.`in`
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
