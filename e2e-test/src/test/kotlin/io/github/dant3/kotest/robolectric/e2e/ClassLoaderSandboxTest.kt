package io.github.dant3.kotest.robolectric.e2e

import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.robolectric.internal.AndroidSandbox.SdkSandboxClassLoader

@RobolectricTest
class ClassLoaderSandboxTest :
    StringSpec({
        "spec class is loaded by SdkSandboxClassLoader" {
            ClassLoaderSandboxTest::class.java.classLoader::class.java shouldBe SdkSandboxClassLoader::class.java
        }

        "thread contextClassLoader is SdkSandboxClassLoader" {
            Thread.currentThread().contextClassLoader::class.java shouldBe SdkSandboxClassLoader::class.java
        }

        "android.os.Build is loaded inside the sandbox" {
            android.os.Build::class.java.classLoader!!::class.java.name shouldContain "SandboxClassLoader"
        }
    })
