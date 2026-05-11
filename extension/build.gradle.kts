plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    `maven-publish`
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("kotest-robolectric-extension")
                description.set("Kotest extension that runs JVM tests inside a Robolectric sandbox.")
                url.set("https://github.com/dant3/kotest-robolectric-extension")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}

dependencies {
    api(libs.kotest.framework.engine)
    api(libs.robolectric)

    implementation(libs.junit4)
    implementation(kotlin("reflect"))

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
