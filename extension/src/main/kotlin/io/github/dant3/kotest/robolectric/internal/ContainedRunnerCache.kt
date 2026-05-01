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
    fun get(config: Config?): ContainedRobolectricRunner {
        ensureSingleSdk(config)
        val key = ConfigKey.from(config)
        return map.getOrPut(key) { ContainedRobolectricRunner(config) }
    }

    private fun ensureSingleSdk(config: Config?) {
        if (config == null) return
        val sdks = config.sdk
        val minSdk = config.minSdk
        val maxSdk = config.maxSdk
        val minIsSet = minSdk != Config.DEFAULT_VALUE_INT
        val maxIsSet = maxSdk != Config.DEFAULT_VALUE_INT

        val isMultiSdk = when {
            sdks.size > 1 -> true
            sdks.size == 1 && sdks[0] == Config.ALL_SDKS -> true
            sdks.isNotEmpty() -> false
            minIsSet && maxIsSet && minSdk != maxSdk -> true
            minIsSet xor maxIsSet -> true
            else -> false
        }

        require(!isMultiSdk) {
            "@Config requests multiple SDK levels (sdk=${sdks.toList()}, minSdk=$minSdk, maxSdk=$maxSdk). " +
                "Multi-SDK execution is not yet supported. " +
                "TODO: support multi-sdk in the future. " +
                "Workaround: declare separate spec classes per SDK."
        }
    }

    private companion object {
        const val DEFAULT_MAX_SIZE = 16
        const val LOAD_FACTOR = 0.75f
    }
}

internal data class ConfigKey(
    val sdk: List<Int>,
    val minSdk: Int,
    val maxSdk: Int,
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
            fontScale = Config.DEFAULT_FONT_SCALE,
            manifest = Config.DEFAULT_VALUE_STRING,
            qualifiers = Config.DEFAULT_QUALIFIERS,
            applicationFqn = "",
            shadowsFqns = emptyList(),
            instrumentedPackages = emptyList(),
        )

        fun from(config: Config?): ConfigKey {
            if (config == null) return DEFAULT
            return ConfigKey(
                sdk = config.sdk.toList(),
                minSdk = config.minSdk,
                maxSdk = config.maxSdk,
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
