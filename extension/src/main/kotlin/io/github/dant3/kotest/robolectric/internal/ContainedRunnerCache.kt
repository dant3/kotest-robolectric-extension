package io.github.dant3.kotest.robolectric.internal

import org.robolectric.annotation.Config
import java.lang.reflect.Method

internal class ContainedRunnerCache(private val maxSize: Int = DEFAULT_MAX_SIZE) {

    private val map: MutableMap<ConfigKey, ContainedRobolectricRunner> = object :
        LinkedHashMap<ConfigKey, ContainedRobolectricRunner>(maxSize, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: Map.Entry<ConfigKey, ContainedRobolectricRunner>): Boolean =
            size > maxSize
    }

    @Synchronized
    fun get(config: Config?, apiLevel: Int = NO_PIN): ContainedRobolectricRunner {
        val key = ConfigKey.from(config, apiLevel)
        return map.getOrPut(key) { ContainedRobolectricRunner(config, apiLevel) }
    }

    private companion object {
        const val DEFAULT_MAX_SIZE = 16
        const val LOAD_FACTOR = 0.75f
    }
}

internal const val NO_PIN: Int = -1

internal data class ConfigKey(
    val sdk: List<Int>,
    val minSdk: Int,
    val maxSdk: Int,
    val pinnedApiLevel: Int,
    val fontScale: Float,
    val manifest: String,
    val qualifiers: String,
    val applicationFqn: String,
    val shadowsFqns: List<String>,
    val instrumentedPackages: List<String>,
) {
    companion object {
        private val DEFAULT = ConfigKey(
            sdk = emptyList(),
            minSdk = Config.DEFAULT_VALUE_INT,
            maxSdk = Config.DEFAULT_VALUE_INT,
            pinnedApiLevel = NO_PIN,
            fontScale = Config.DEFAULT_FONT_SCALE,
            manifest = Config.DEFAULT_VALUE_STRING,
            qualifiers = Config.DEFAULT_QUALIFIERS,
            applicationFqn = "",
            shadowsFqns = emptyList(),
            instrumentedPackages = emptyList(),
        )

        fun from(config: Config?, apiLevel: Int): ConfigKey {
            if (config == null) return DEFAULT.copy(pinnedApiLevel = apiLevel)
            return ConfigKey(
                sdk = config.sdk.toList(),
                minSdk = config.minSdk,
                maxSdk = config.maxSdk,
                pinnedApiLevel = apiLevel,
                fontScale = config.fontScale,
                manifest = config.manifest,
                qualifiers = config.qualifiers,
                applicationFqn = (APPLICATION_METHOD.invoke(config) as Class<*>).name,
                shadowsFqns = shadowsFqns(config),
                instrumentedPackages = config.instrumentedPackages.toList().sorted(),
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun shadowsFqns(config: Config): List<String> =
            (SHADOWS_METHOD.invoke(config) as Array<Class<*>>).map { it.name }.sorted()

        // Reflection is required because Config.application returns Class<? extends android.app.Application>,
        // and the Application class is not on the classpath of this pure-JVM module. Reflection bypasses
        // Kotlin's compile-time type resolution.
        private val APPLICATION_METHOD: Method = Config::class.java.getMethod("application")
        private val SHADOWS_METHOD: Method = Config::class.java.getMethod("shadows")
    }
}
