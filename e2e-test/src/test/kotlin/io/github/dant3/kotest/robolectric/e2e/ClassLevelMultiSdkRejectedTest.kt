package io.github.dant3.kotest.robolectric.e2e

import io.github.dant3.kotest.robolectric.RobolectricExtension
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import org.robolectric.annotation.Config

class ClassLevelMultiSdkRejectedTest :
    StringSpec({
        val extension = RobolectricExtension()

        "@Config(sdk = [array]) with multiple values is rejected with withSdks hint" {
            val ex = shouldThrow<IllegalArgumentException> {
                extension.instantiate(MultiSdkArraySpec::class)
            }
            ex.message!!.shouldContain("multiple SDK levels")
            ex.message!!.shouldContain("withSdks")
        }

        "@Config(minSdk, maxSdk) range is rejected" {
            shouldThrow<IllegalArgumentException> {
                extension.instantiate(MultiSdkRangeSpec::class)
            }
        }

        "@Config(sdk = [ALL_SDKS]) is rejected" {
            shouldThrow<IllegalArgumentException> {
                extension.instantiate(MultiSdkAllSpec::class)
            }
        }

        "@Config(minSdk) without maxSdk is rejected" {
            shouldThrow<IllegalArgumentException> {
                extension.instantiate(MultiSdkOpenRangeSpec::class)
            }
        }
    })

@RobolectricTest
@Config(sdk = [21, 28, 30])
private abstract class MultiSdkArraySpec : StringSpec()

@RobolectricTest
@Config(minSdk = 21, maxSdk = 28)
private abstract class MultiSdkRangeSpec : StringSpec()

@RobolectricTest
@Config(sdk = [Config.ALL_SDKS])
private abstract class MultiSdkAllSpec : StringSpec()

@RobolectricTest
@Config(minSdk = 21)
private abstract class MultiSdkOpenRangeSpec : StringSpec()
