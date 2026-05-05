package io.github.dant3.kotest.robolectric.internal

import org.robolectric.pluginapi.SdkPicker
import org.robolectric.pluginapi.UsesSdk
import org.robolectric.pluginapi.config.ConfigurationStrategy
import java.lang.reflect.Method

internal object SdkResolver {

    private val MARKER_METHOD: Method = Any::class.java.getMethod("toString")

    fun resolveSdks(specClass: Class<*>): List<Int> {
        val injector = ContainedRobolectricRunner.defaultInjectorBuilder().build()
        val picker = injector.getInstance(SdkPicker::class.java)
        val configurationStrategy = injector.getInstance(ConfigurationStrategy::class.java)
        // HierarchicalConfigurationStrategy requires a non-null Method. The spec class has no
        // JUnit-style @Test methods, so we pass Object::toString as a marker — it carries no
        // Robolectric annotations, leaving class-level @Config as the sole input.
        val configuration = configurationStrategy.getConfig(specClass, MARKER_METHOD)
        return picker.selectSdks(configuration, StubUsesSdk).map { it.apiLevel }
    }

    private object StubUsesSdk : UsesSdk {
        override fun getMinSdkVersion(): Int = 0
        override fun getTargetSdkVersion(): Int = 0
        override fun getMaxSdkVersion(): Int? = null
    }
}
