package io.github.dant3.kotest.robolectric.e2e

import android.os.Looper
import io.github.dant3.kotest.robolectric.RobolectricExtension
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

@RobolectricTest
@ApplyExtension(RobolectricExtension::class)
class MainThreadLooperTest :
    StringSpec({
        "тест выполняется на main looper-потоке Robolectric" {
            val mainLooper = Looper.getMainLooper()
            mainLooper.shouldNotBeNull()
            Thread.currentThread() shouldBe mainLooper.thread
        }
    })
