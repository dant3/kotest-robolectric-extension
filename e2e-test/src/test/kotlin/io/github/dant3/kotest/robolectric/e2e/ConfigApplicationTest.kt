package io.github.dant3.kotest.robolectric.e2e

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import org.robolectric.annotation.Config

@RobolectricTest
@Config(application = TestApplication::class)
class ConfigApplicationTest :
    StringSpec({
        "@Config(application = TestApplication::class) substitutes the Application instance" {
            val app = ApplicationProvider.getApplicationContext<Application>()
            app.shouldBeInstanceOf<TestApplication>()
        }
    })
