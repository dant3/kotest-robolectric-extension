package io.github.dant3.kotest.robolectric.internal

internal object SharedRunnerCache {
    private val cache = ContainedRunnerCache()

    fun get(spec: SpecConfiguration, sdk: Int = NO_PIN): ContainedRobolectricRunner = cache.get(spec, sdk)
}
