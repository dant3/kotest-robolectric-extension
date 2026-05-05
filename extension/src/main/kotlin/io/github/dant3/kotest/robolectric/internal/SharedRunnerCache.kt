package io.github.dant3.kotest.robolectric.internal

import org.robolectric.annotation.Config

internal object SharedRunnerCache {
    private val cache = ContainedRunnerCache()

    fun get(config: Config?, sdk: Int = NO_PIN): ContainedRobolectricRunner = cache.get(config, sdk)
}
