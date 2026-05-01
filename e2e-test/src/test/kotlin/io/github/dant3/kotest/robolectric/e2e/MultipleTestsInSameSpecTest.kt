package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

@RobolectricTest
class MultipleTestsInSameSpecTest :
    StringSpec({
        "тест 1: SDK_INT доступен" {
            Build.VERSION.SDK_INT shouldBeGreaterThanOrEqual 21
        }

        "тест 2: classloader не меняется между тестами одного spec" {
            val cl = Thread.currentThread().contextClassLoader
            cl.shouldNotBeNull()
            cl::class.java.simpleName shouldBe "SdkSandboxClassLoader"
        }

        "тест 3: SDK_INT всё ещё доступен" {
            Build.VERSION.SDK_INT shouldBeGreaterThanOrEqual 21
        }
    })
