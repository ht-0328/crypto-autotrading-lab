plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "com.crypto.autotrading"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Client
    val ktorVersion = "3.0.0"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("ch.qos.logback:logback-classic:1.5.11")

    // Jackson / YAML for config
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.1")

    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("com.lemonappdev:konsist:0.15.1")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("cryptoautotrading.presentation.MainKt")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Test> {
    systemProperty("file.encoding", "UTF-8")
}

tasks.withType<JavaExec> {
    systemProperty("file.encoding", "UTF-8")
}

tasks.register<JavaExec>("exportKlinesCsv") {
    group = "application"
    description = "過去K線CSVを作成します"
    mainClass.set("cryptoautotrading.presentation.KlineCsvExportMainKt")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperty("file.encoding", "UTF-8")
}
