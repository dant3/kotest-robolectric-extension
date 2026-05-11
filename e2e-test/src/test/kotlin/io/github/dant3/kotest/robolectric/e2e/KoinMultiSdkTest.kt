package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.ExperimentalRobolectricKotestApi
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.github.dant3.kotest.robolectric.withSdks
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class GreetingService(private val greeting: String) {
    fun greet(): String = greeting
}

@RobolectricTest
@OptIn(ExperimentalRobolectricKotestApi::class)
class KoinMultiSdkTest :
    StringSpec({
        withSdks(
            "Koin DI works under each Robolectric SDK",
            23,
            28,
            30,
            setup = {
                startKoin {
                    modules(
                        module {
                            factory { GreetingService("hello") }
                        },
                    )
                }
            },
            teardown = { stopKoin() },
        ) { sdk ->
            Build.VERSION.SDK_INT shouldBe sdk
            val service = object : KoinComponent {}.get<GreetingService>()
            service.greet() shouldBe "hello"
        }
    })
