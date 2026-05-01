package io.github.dant3.kotest.robolectric.e2e

import io.github.dant3.kotest.robolectric.RobolectricExtension
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@ApplyExtension(RobolectricExtension::class)
class PlainSpecTest :
    StringSpec({
        "spec без @RobolectricTest не должен ломаться при наличии экстеншна" {
            1 + 1 shouldBe 2
        }
    })
