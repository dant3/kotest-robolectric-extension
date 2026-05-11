plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
}

android {
    namespace = "io.github.dant3.kotest.robolectric.e2e"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.androidMinSdk.get().toInt()
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    // Pre-declare singleVariant so JitPack does not auto-inject its Groovy-syntax variant
    // into this Kotlin DSL file. This module is not published; the block is here purely to
    // keep JitPack's file modifier from corrupting the script.
    publishing {
        singleVariant("release") {}
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(project(":extension"))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.koin.core)
    testImplementation(libs.koin.test)

    detektPlugins(libs.gradlePlugin.detekt.formatting)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootDir.resolve("gradle/detekt.yml"))
    source.setFrom(files("src/main/kotlin", "src/test/kotlin"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}
