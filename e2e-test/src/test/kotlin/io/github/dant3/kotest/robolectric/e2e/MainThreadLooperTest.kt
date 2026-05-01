package io.github.dant3.kotest.robolectric.e2e

import android.os.Looper
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

@RobolectricTest
class MainThreadLooperTest :
    StringSpec({
        "test runs on Robolectric's main looper thread" {
            val mainLooper = Looper.getMainLooper()
            mainLooper.shouldNotBeNull()
            Thread.currentThread() shouldBe mainLooper.thread
        }
    })
