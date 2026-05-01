package io.github.dant3.kotest.robolectric.e2e

import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.robolectric.internal.AndroidSandbox.SdkSandboxClassLoader

@RobolectricTest
class ClassLoaderSandboxTest :
    StringSpec({
        "spec-класс загружен SdkSandboxClassLoader" {
            ClassLoaderSandboxTest::class.java.classLoader::class.java shouldBe SdkSandboxClassLoader::class.java
        }

        "contextClassLoader потока — SdkSandboxClassLoader" {
            Thread.currentThread().contextClassLoader::class.java shouldBe SdkSandboxClassLoader::class.java
        }

        "android.os.Build загружен внутри песочницы" {
            android.os.Build::class.java.classLoader!!::class.java.name shouldContain "SandboxClassLoader"
        }
    })
