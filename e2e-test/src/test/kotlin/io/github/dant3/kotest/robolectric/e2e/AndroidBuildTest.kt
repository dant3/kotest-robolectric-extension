package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldNotBe

@RobolectricTest
class AndroidBuildTest :
    StringSpec({
        "Build.VERSION.SDK_INT is available inside the Robolectric sandbox" {
            Build.VERSION.SDK_INT shouldBeGreaterThanOrEqual 21
        }

        "Build.MANUFACTURER is available and not null" {
            Build.MANUFACTURER shouldNotBe null
        }
    })
