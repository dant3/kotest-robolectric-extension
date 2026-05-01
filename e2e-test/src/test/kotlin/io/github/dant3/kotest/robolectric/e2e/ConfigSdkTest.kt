package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.robolectric.annotation.Config

@RobolectricTest
@Config(sdk = [Build.VERSION_CODES.R])
class ConfigSdkTest :
    StringSpec({
        "@Config(sdk = R) применяет SDK 30" {
            Build.VERSION.SDK_INT shouldBe Build.VERSION_CODES.R
        }
    })
