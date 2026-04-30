plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
}

group = "io.github.dant3.kotest.robolectric"
version = "0.1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    api(libs.kotest.framework.engine)
    api(libs.robolectric)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)

    detektPlugins(libs.gradlePlugin.detekt.formatting)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootDir.resolve("gradle/detekt.yml"))
    source.setFrom(files("src/main/kotlin", "src/test/kotlin"))
}

tasks.test {
    useJUnitPlatform()
}
