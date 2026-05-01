package io.github.dant3.kotest.robolectric.internal

import org.junit.Test
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.pluginapi.config.ConfigurationStrategy
import org.robolectric.util.inject.Injector
import java.lang.reflect.Method

internal class ContainedRobolectricRunner(config: Config?) :
    RobolectricTestRunner(PlaceholderTest::class.java, buildInjector(config)) {

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

    private companion object {
        fun buildInjector(config: Config?): Injector {
            val builder = defaultInjector()
            if (config != null) {
                val baseStrategy = defaultInjector().build().getInstance(ConfigurationStrategy::class.java)
                builder.bind(ConfigurationStrategy::class.java, MergingConfigurationStrategy(baseStrategy, config))
            }
            return builder.build()
        }
    }

    private class MergingConfigurationStrategy(
        private val delegate: ConfigurationStrategy,
        private val userConfig: Config,
    ) : ConfigurationStrategy {

        override fun getConfig(testClass: Class<*>, method: Method?): ConfigurationStrategy.Configuration {
            val base = delegate.getConfig(testClass, method)
            val baseConfig = base.get(Config::class.java) ?: Config.Builder.defaults().build()
            val merged: Config = Config.Builder(baseConfig).overlay(userConfig).build()
            return MergedConfiguration(base, merged)
        }
    }

    private class MergedConfiguration(
        private val base: ConfigurationStrategy.Configuration,
        private val mergedConfig: Config,
    ) : ConfigurationStrategy.Configuration {

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> get(clazz: Class<T>): T? =
            if (clazz == Config::class.java) mergedConfig as T else base.get(clazz)

        override fun keySet(): Collection<Class<*>> = base.keySet()

        override fun map(): Map<Class<*>, Any> =
            base.map().toMutableMap().also { it[Config::class.java] = mergedConfig }
    }
}
