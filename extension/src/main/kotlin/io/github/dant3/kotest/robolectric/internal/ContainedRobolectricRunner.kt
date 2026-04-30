package io.github.dant3.kotest.robolectric.internal

import org.junit.Test
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.InstrumentationConfiguration

internal class ContainedRobolectricRunner : RobolectricTestRunner(PlaceholderTest::class.java) {

    private val placeholderMethod: FrameworkMethod = children[0]

    val sdkEnvironment = getSandbox(placeholderMethod).also {
        configureSandbox(it, placeholderMethod)
    }

    private val bootstrapMethod = sdkEnvironment
        .bootstrappedClass<Any>(PlaceholderTest::class.java)
        .getMethod(PlaceholderTest::bootstrap.name)

    fun containedBefore() {
        beforeTest(sdkEnvironment, placeholderMethod, bootstrapMethod)
    }

    fun containedAfter() {
        try {
            afterTest(placeholderMethod, bootstrapMethod)
        } finally {
            finallyAfterTest(placeholderMethod)
        }
    }

    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration =
        InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotAcquirePackage("io.kotest")
            .build()

    class PlaceholderTest {
        @Test
        fun bootstrap() {
            // placeholder — служит только для получения FrameworkMethod, чтобы проинициализировать sandbox
        }
    }
}
