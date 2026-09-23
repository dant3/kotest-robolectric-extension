package io.github.dant3.kotest.robolectric.e2e

import android.app.Application
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.robolectric.annotation.Config

@RobolectricTest
@Config(sdk = [Build.VERSION_CODES.R])
abstract class RobolectricBaseSpec(body: RobolectricBaseSpec.() -> Unit) : StringSpec() {
    init {
        body()
    }
}

class InheritedAnnotationsTest :
    RobolectricBaseSpec({
        "@RobolectricTest on an abstract base spec runs the subclass in the sandbox" {
            ApplicationProvider.getApplicationContext<Application>() shouldBe
                ApplicationProvider.getApplicationContext<Application>()
        }

        "@Config on an abstract base spec applies to the subclass" {
            Build.VERSION.SDK_INT shouldBe Build.VERSION_CODES.R
        }
    })

@Config(qualifiers = "fr")
class InheritedConfigOverlayTest :
    RobolectricBaseSpec({
        "subclass @Config is overlaid on the base @Config" {
            Build.VERSION.SDK_INT shouldBe Build.VERSION_CODES.R
            val app = ApplicationProvider.getApplicationContext<Application>()
            app.resources.configuration.locales[0].language shouldBe "fr"
        }
    })
