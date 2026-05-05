package io.github.dant3.kotest.robolectric.internal

internal object SdkBootstrapContext {
    private val current = ThreadLocal<Int?>()

    val currentSdk: Int? get() = current.get()

    fun <T> withSdk(sdk: Int, block: () -> T): T {
        val previous = current.get()
        current.set(sdk)
        try {
            return block()
        } finally {
            if (previous == null) current.remove() else current.set(previous)
        }
    }
}
