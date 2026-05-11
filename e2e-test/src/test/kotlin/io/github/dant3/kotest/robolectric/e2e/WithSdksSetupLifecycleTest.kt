package io.github.dant3.kotest.robolectric.e2e

import android.os.Build
import io.github.dant3.kotest.robolectric.ExperimentalRobolectricKotestApi
import io.github.dant3.kotest.robolectric.RobolectricTest
import io.github.dant3.kotest.robolectric.withSdks
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies the setup/teardown lifecycle of the experimental withSdks overload.
 *
 * State is tracked via [System] properties because they live on a single
 * JVM-global [java.util.Properties] instance — sandbox classloaders share it via the
 * parent-loaded `System` class, which is the only easy cross-sandbox storage we can
 * rely on here. (Counters declared in the spec body are per-bootstrap because the
 * spec body is re-evaluated under each per-SDK sandbox.)
 */
private const val LOG_KEY = "kotest-robolectric.lifecycle.log"

private fun appendLog(event: String) {
    System.setProperty(LOG_KEY, System.getProperty(LOG_KEY, "") + event + ";")
}

@RobolectricTest
@OptIn(ExperimentalRobolectricKotestApi::class)
class WithSdksSetupLifecycleTest :
    StringSpec({
        beforeSpec { System.clearProperty(LOG_KEY) }

        withSdks(
            "ordered events",
            23,
            28,
            setup = { appendLog("setup[${Build.VERSION.SDK_INT}]") },
            teardown = { appendLog("teardown[${Build.VERSION.SDK_INT}]") },
        ) { sdk ->
            appendLog("body[$sdk]")
            Build.VERSION.SDK_INT shouldBe sdk
        }

        "setup and teardown run in order around each per-SDK body, inside the sandbox" {
            System.getProperty(LOG_KEY) shouldBe
                "setup[23];body[23];teardown[23];setup[28];body[28];teardown[28];"
        }
    })
