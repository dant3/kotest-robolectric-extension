package io.github.dant3.kotest.robolectric.e2e

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private fun prefs(): SharedPreferences = ApplicationProvider.getApplicationContext<Application>()
    .getSharedPreferences("isolation", Context.MODE_PRIVATE)

private fun SharedPreferences.claim(owner: String): String? =
    getString("owner", null).also { edit().putString("owner", owner).commit() }

@RobolectricTest
class SingleInstanceSharesStateTest :
    FunSpec({
        test("first test leaves state behind") {
            prefs().claim("first").shouldBeNull()
        }

        test("second test sees it: one Application per spec") {
            prefs().claim("second") shouldBe "first"
        }
    })

@RobolectricTest
class InstancePerRootIsolationTest :
    FunSpec({
        isolationMode = IsolationMode.InstancePerRoot

        test("root 1 starts from a fresh Application") {
            prefs().claim("root 1").shouldBeNull()
        }

        test("root 2 starts from a fresh Application") {
            prefs().claim("root 2").shouldBeNull()
        }

        afterSpec {
            ApplicationProvider.getApplicationContext<Application>().shouldNotBeNull()
        }
    })

@RobolectricTest
class InstancePerTestIsolationTest :
    FunSpec({
        isolationMode = IsolationMode.InstancePerTest

        context("container") {
            test("leaf 1 starts from a fresh Application") {
                prefs().claim("leaf 1").shouldBeNull()
            }

            test("leaf 2 starts from a fresh Application") {
                prefs().claim("leaf 2").shouldBeNull()
            }
        }

        afterSpec {
            ApplicationProvider.getApplicationContext<Application>().shouldNotBeNull()
        }
    })
