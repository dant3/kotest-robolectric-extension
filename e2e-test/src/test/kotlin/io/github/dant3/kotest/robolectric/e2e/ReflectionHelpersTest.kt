package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.robolectric.util.ReflectionHelpers

@RobolectricTest
class ReflectionHelpersTest :
    StringSpec({
        "ReflectionHelpers.setStaticField bypasses the final modifier on an instrumented class" {
            ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "expected value")
            Build.MODEL shouldBe "expected value"
        }
    })
