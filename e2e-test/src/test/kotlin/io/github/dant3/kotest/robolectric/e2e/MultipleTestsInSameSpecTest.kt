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
        "test 1: SDK_INT is available" {
            Build.VERSION.SDK_INT shouldBeGreaterThanOrEqual 21
        }

        "test 2: classloader is stable across tests in the same spec" {
            val cl = Thread.currentThread().contextClassLoader
            cl.shouldNotBeNull()
            cl::class.java.simpleName shouldBe "SdkSandboxClassLoader"
        }

        "test 3: SDK_INT is still available" {
            Build.VERSION.SDK_INT shouldBeGreaterThanOrEqual 21
        }
    })
