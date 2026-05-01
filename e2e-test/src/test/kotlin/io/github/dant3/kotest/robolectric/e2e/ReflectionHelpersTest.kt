package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.RobolectricExtension
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.robolectric.util.ReflectionHelpers

@RobolectricTest
@ApplyExtension(RobolectricExtension::class)
class ReflectionHelpersTest :
    StringSpec({
        "ReflectionHelpers.setStaticField обходит final-модификатор инструментированного класса" {
            ReflectionHelpers.setStaticField(Build::class.java, "MODEL", "expected value")
            Build.MODEL shouldBe "expected value"
        }
    })
