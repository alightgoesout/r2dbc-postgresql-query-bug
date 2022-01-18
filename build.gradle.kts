plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.postgresql:r2dbc-postgresql:0.9.0.RELEASE")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.0")
}

application {
    mainClass.set("org.example.pgbug.MainKt")
}
