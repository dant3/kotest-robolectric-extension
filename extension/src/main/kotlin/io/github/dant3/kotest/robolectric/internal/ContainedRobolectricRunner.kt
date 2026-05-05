package io.github.dant3.kotest.robolectric.internal

import java.lang.reflect.Method
import org.junit.Test
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.pluginapi.config.ConfigurationStrategy
import org.robolectric.util.inject.Injector

internal class ContainedRobolectricRunner(config: Config?, apiLevel: Int = NO_PIN) :
    RobolectricTestRunner(PlaceholderTest::class.java, buildInjector(config, apiLevel)) {
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

    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration {
        // Public-API classes are pinned individually because doNotAcquirePackage uses prefix
        // matching (`name.startsWith(prefix + ".")`) and would also catch user spec classes
        // that happen to live under our top-level package.
        val builder = InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotAcquirePackage("io.kotest")
            .doNotAcquirePackage("io.github.dant3.kotest.robolectric.internal")
            .doNotAcquirePackage("org.robolectric.annotation")
        OUR_PUBLIC_CLASS_NAMES.forEach { builder.doNotAcquireClass(it) }
        return builder.build()
    }

    class PlaceholderTest {
        @Test
        fun bootstrap() {
            // Placeholder used only to obtain a FrameworkMethod for sandbox initialization.
        }
    }

    internal companion object {
        // Pinned public-API classes — must stay parent-loaded so the cache, runner registry
        // and SDK bootstrap context can be shared across sandboxes.
        private val OUR_PUBLIC_CLASS_NAMES = listOf(
            "io.github.dant3.kotest.robolectric.RobolectricExtension",
            "io.github.dant3.kotest.robolectric.RobolectricTest",
            "io.github.dant3.kotest.robolectric.SdksKt",
        )

        fun defaultInjectorBuilder(): Injector.Builder = defaultInjector()

        fun buildInjector(config: Config?, apiLevel: Int): Injector {
            val pin = if (apiLevel != NO_PIN) Config.Builder().setSdk(apiLevel).build() else null
            val effective = when {
                config == null && pin == null -> null
                config == null -> pin
                pin == null -> config
                else -> Config.Builder(config).overlay(pin).build()
            }
            if (effective == null) return defaultInjector().build()
            val baseStrategy = defaultInjector().build().getInstance(ConfigurationStrategy::class.java)
            return defaultInjector()
                .bind(ConfigurationStrategy::class.java, MergingConfigurationStrategy(baseStrategy, effective))
                .build()
        }
    }

    private class MergingConfigurationStrategy(
        private val delegate: ConfigurationStrategy,
        private val userConfig: Config,
    ) : ConfigurationStrategy {
        override fun getConfig(testClass: Class<*>, method: Method?): ConfigurationStrategy.Configuration {
            val base = delegate.getConfig(testClass, method)
            val baseConfig = requireNotNull(base.get(Config::class.java)) {
                "ConfigurationStrategy returned no Config — Robolectric default plugin chain not loaded?"
            }
            val merged: Config = Config.Builder(baseConfig).overlay(userConfig).build()
            return MergedConfiguration(base, merged)
        }
    }

    private class MergedConfiguration(
        private val base: ConfigurationStrategy.Configuration,
        private val mergedConfig: Config,
    ) : ConfigurationStrategy.Configuration {
        private val mergedMap: Map<Class<*>, Any> by lazy {
            base.map().toMutableMap().also { it[Config::class.java] = mergedConfig }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> get(clazz: Class<T>): T? =
            if (clazz == Config::class.java) mergedConfig as T else base.get(clazz)

        override fun keySet(): Collection<Class<*>> = base.keySet()

        override fun map(): Map<Class<*>, Any> = mergedMap
    }
}
