package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

class AndroidGreeter {
    fun greet(): String = "Hello from SDK ${Build.VERSION.SDK_INT}"
}

@RobolectricTest
class KoinSingleSdkTest :
    StringSpec(), KoinTest {
    private val greeter: AndroidGreeter by inject()

    init {
        beforeSpec {
            startKoin {
                modules(
                    module {
                        single { AndroidGreeter() }
                    },
                )
            }
        }
        afterSpec { stopKoin() }

        "Koin-injected component sees the Robolectric sandbox" {
            Build.VERSION.SDK_INT shouldBeGreaterThanOrEqual Build.VERSION_CODES.M
            greeter.greet() shouldBe "Hello from SDK ${Build.VERSION.SDK_INT}"
        }
    }
}
