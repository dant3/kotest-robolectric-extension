package io.github.dant3.kotest.robolectric.e2e

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

@RobolectricTest
class ApplicationProviderTest :
    StringSpec({
        "ApplicationProvider returns an Application context" {
            val app = ApplicationProvider.getApplicationContext<Context>()
            app.shouldNotBeNull()
            app.shouldBeInstanceOf<Application>()
        }

        "repeated call in the same spec returns the same Application instance" {
            val first = ApplicationProvider.getApplicationContext<Application>()
            val second = ApplicationProvider.getApplicationContext<Application>()
            first.shouldNotBeNull()
            second.shouldNotBeNull()
        }
    })
