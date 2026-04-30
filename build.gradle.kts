plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    group = "io.github.dant3.kotest.robolectric"
    version = "0.1.0-SNAPSHOT"
}
