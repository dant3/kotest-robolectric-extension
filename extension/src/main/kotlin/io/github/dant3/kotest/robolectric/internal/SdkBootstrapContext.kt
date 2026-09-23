package io.github.dant3.kotest.robolectric.internal

import io.kotest.core.spec.RootTest

internal object SdkBootstrapContext {
    private data class Frame(val sdk: Int, val captured: MutableList<RootTest> = mutableListOf())

    private val current = ThreadLocal<Frame?>()

    val currentSdk: Int? get() = current.get()?.sdk

    /**
     * Runs [block] with [sdk] as the active bootstrap SDK and returns the root tests that
     * [capture] received meanwhile. Capturing here, rather than reading them back from the
     * bootstrapped spec, keeps us off the spec's test-listing API, which differs between
     * Kotest versions (`rootTests()` in 6.1, `tests()` in 6.2).
     */
    fun withSdk(sdk: Int, block: () -> Unit): List<RootTest> {
        val previous = current.get()
        val frame = Frame(sdk)
        current.set(frame)
        try {
            block()
            return frame.captured
        } finally {
            if (previous == null) current.remove() else current.set(previous)
        }
    }

    fun capture(test: RootTest) {
        checkNotNull(current.get()) { "capture() called outside of withSdk()" }.captured += test
    }
}
