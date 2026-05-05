package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import org.robolectric.annotation.Config

@RobolectricTest
@Config(sdk = [Build.VERSION_CODES.M, Build.VERSION_CODES.P, Build.VERSION_CODES.R])
class MultiSdkExpansionTest :
    StringSpec({
        "spec runs once per configured SDK" {
            setOf(
                Build.VERSION_CODES.M,
                Build.VERSION_CODES.P,
                Build.VERSION_CODES.R,
            ) shouldContain Build.VERSION.SDK_INT
        }
    })
