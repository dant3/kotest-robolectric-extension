package io.github.dant3.kotest.robolectric.internal

import org.robolectric.annotation.Config

internal object MultiSdkErrors {
    fun checkClassLevelSingleSdk(specClass: Class<*>, config: Config?) {
        if (config == null) return
        val sdks = config.sdk
        val minSdk = config.minSdk
        val maxSdk = config.maxSdk
        val minIsSet = minSdk != Config.DEFAULT_VALUE_INT
        val maxIsSet = maxSdk != Config.DEFAULT_VALUE_INT

        when {
            sdks.size > 1 ->
                fail(specClass, "sdk=${sdks.toList()}")

            sdks.size == 1 && sdks[0] == Config.ALL_SDKS ->
                fail(specClass, "sdk=[Config.ALL_SDKS]")

            sdks.isEmpty() && minIsSet && maxIsSet && minSdk != maxSdk ->
                fail(specClass, "minSdk=$minSdk, maxSdk=$maxSdk")

            sdks.isEmpty() && minIsSet xor maxIsSet -> {
                val descriptor = if (minIsSet) "minSdk=$minSdk (no maxSdk)" else "maxSdk=$maxSdk (no minSdk)"
                fail(specClass, descriptor)
            }
        }
    }

    private fun fail(specClass: Class<*>, descriptor: String): Nothing = throw IllegalArgumentException(
        """
        @Config on ${specClass.name} declares multiple SDK levels at the class level
        ($descriptor), which is not supported by kotest-robolectric-extension.

        The class-level @Config must pin a single SDK (or be omitted to use the
        Robolectric default). To run tests across multiple SDKs, use the withSdks
        DSL inside the spec body:

            @RobolectricTest
            class FooTest : StringSpec({
                withSdks("Build.VERSION reports", 23, 28, 30) { sdk ->
                    Build.VERSION.SDK_INT shouldBe sdk
                }
            })

        See README "Running across multiple SDKs" for details.
        """.trimIndent(),
    )
}
