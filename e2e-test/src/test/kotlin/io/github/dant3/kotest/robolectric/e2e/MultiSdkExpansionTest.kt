package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.ExperimentalRobolectricKotestApi
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.github.dant3.kotest.robolectric.withSdks
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

@RobolectricTest
@OptIn(ExperimentalRobolectricKotestApi::class)
class MultiSdkExpansionTest :
    StringSpec({
        "single-SDK test runs once on default sandbox" {
            Build.VERSION.SDK_INT shouldBeGreaterThanOrEqual Build.VERSION_CODES.M
        }

        withSdks(
            "Build.VERSION reports",
            Build.VERSION_CODES.M,
            Build.VERSION_CODES.P,
            Build.VERSION_CODES.R,
        ) { sdk ->
            Build.VERSION.SDK_INT shouldBe sdk
        }

        withSdks(
            Build.VERSION_CODES.M,
            Build.VERSION_CODES.R,
            nameFn = { "level $it support" },
        ) { sdk ->
            Build.VERSION.SDK_INT shouldBe sdk
        }
    })
