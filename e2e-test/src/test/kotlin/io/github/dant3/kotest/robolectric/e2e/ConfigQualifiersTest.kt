package io.github.dant3.kotest.robolectric.e2e

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.robolectric.annotation.Config

@RobolectricTest
@Config(qualifiers = "fr")
class ConfigQualifiersTest :
    StringSpec({
        "@Config(qualifiers = fr) применяет французскую локаль к ресурсам" {
            val app = ApplicationProvider.getApplicationContext<Application>()
            app.resources.configuration.locales[0].language shouldBe "fr"
        }
    })
