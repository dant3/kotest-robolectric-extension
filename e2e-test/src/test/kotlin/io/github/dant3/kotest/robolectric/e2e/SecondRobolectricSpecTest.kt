package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.RobolectricExtension
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual

@RobolectricTest
@ApplyExtension(RobolectricExtension::class)
class SecondRobolectricSpecTest :
    StringSpec({
        "несколько spec с @RobolectricTest независимо инициализируют sandbox" {
            Build.VERSION.SDK_INT shouldBeGreaterThanOrEqual 21
        }
    })
