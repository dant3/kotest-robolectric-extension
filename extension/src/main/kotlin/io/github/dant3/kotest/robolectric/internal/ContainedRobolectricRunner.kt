package io.github.dant3.kotest.robolectric.internal

import java.lang.reflect.Method
import org.junit.Test
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.internal.bytecode.InstrumentationConfiguration
import org.robolectric.pluginapi.config.ConfigurationStrategy
import org.robolectric.util.inject.Injector

internal class ContainedRobolectricRunner(spec: SpecConfiguration, apiLevel: Int = NO_PIN) :
    RobolectricTestRunner(PlaceholderTest::class.java, buildInjector(spec, apiLevel)) {
    private val placeholderMethod: FrameworkMethod = children[0]

    val sdkEnvironment = getSandbox(placeholderMethod).also {
        configureSandbox(it, placeholderMethod)
    }

    private val bootstrapMethod = sdkEnvironment
        .bootstrappedClass<Any>(PlaceholderTest::class.java)
        .getMethod(PlaceholderTest::bootstrap.name)

    // Lifecycles on one runner nest: under InstancePerRoot/InstancePerTest Kotest runs the pipeline
    // of each fresh spec instance inside the seed instance's pipeline. Every level gets a fresh
    // environment, and leaving an inner level sets up a fresh one for the enclosing level, so
    // Android state is always live while any level is active.
    private var depth = 0

    fun containedBefore() {
        if (depth > 0) tearDown()
        beforeTest(sdkEnvironment, placeholderMethod, bootstrapMethod)
        depth++
    }

    fun containedAfter() {
        depth--
        tearDown()
        if (depth > 0) beforeTest(sdkEnvironment, placeholderMethod, bootstrapMethod)
    }

    private fun tearDown() {
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

        private val defaultStrategy: ConfigurationStrategy by lazy {
            defaultInjector().build().getInstance(ConfigurationStrategy::class.java)
        }

        private val placeholderMethod: Method = PlaceholderTest::class.java.getMethod(PlaceholderTest::bootstrap.name)

        /**
         * Everything Robolectric's configurers resolve for [specClass] besides `@Config` — `@GraphicsMode`,
         * `@LooperMode`, `@SQLiteMode` and the rest, from the spec, its supertypes, its package and the system
         * properties. The sandbox is built for [PlaceholderTest], which carries none of them, so without this
         * every such annotation on a spec would be ignored without a word.
         */
        fun modesOf(specClass: Class<*>): Map<Class<*>, Any> =
            defaultStrategy.getConfig(specClass, placeholderMethod).map().filterKeys { it != Config::class.java }

        fun buildInjector(spec: SpecConfiguration, apiLevel: Int): Injector {
            val pin = if (apiLevel != NO_PIN) Config.Builder().setSdk(apiLevel).build() else null
            val effective = when {
                spec.config == null -> pin
                pin == null -> spec.config
                else -> Config.Builder(spec.config).overlay(pin).build()
            }
            return defaultInjector()
                .bind(ConfigurationStrategy::class.java, SpecConfigurationStrategy(defaultStrategy, effective, spec.modes))
                .build()
        }
    }

    /** Resolves the placeholder's configuration, then lays the spec's own `@Config` and modes over it. */
    private class SpecConfigurationStrategy(
        private val delegate: ConfigurationStrategy,
        private val userConfig: Config?,
        private val modes: Map<Class<*>, Any>,
    ) : ConfigurationStrategy {
        override fun getConfig(testClass: Class<*>, method: Method?): ConfigurationStrategy.Configuration {
            val base = delegate.getConfig(testClass, method)
            val baseConfig = requireNotNull(base.get(Config::class.java)) {
                "ConfigurationStrategy returned no Config — Robolectric default plugin chain not loaded?"
            }
            val merged = if (userConfig == null) baseConfig else Config.Builder(baseConfig).overlay(userConfig).build()
            return SpecConfigurationResult(base.map() + modes + (Config::class.java to merged))
        }
    }

    private class SpecConfigurationResult(private val values: Map<Class<*>, Any>) : ConfigurationStrategy.Configuration {
        @Suppress("UNCHECKED_CAST") // keyed by the class of its own value, as Configuration.map() is
        override fun <T : Any?> get(clazz: Class<T>): T? = values[clazz] as T?

        override fun keySet(): Collection<Class<*>> = values.keys

        override fun map(): Map<Class<*>, Any> = values
    }
}
