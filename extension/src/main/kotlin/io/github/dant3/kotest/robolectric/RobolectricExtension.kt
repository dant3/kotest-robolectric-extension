package io.github.dant3.kotest.robolectric

import io.github.dant3.kotest.robolectric.internal.MultiSdkErrors
import io.github.dant3.kotest.robolectric.internal.SharedRunnerCache
import io.github.dant3.kotest.robolectric.internal.SpecAnnotations
import io.kotest.core.extensions.ConstructorExtension
import io.kotest.core.extensions.SpecExtension
import io.kotest.core.spec.Spec
import kotlin.reflect.KClass

public class RobolectricExtension :
    ConstructorExtension,
    SpecExtension {
    override fun <T : Spec> instantiate(clazz: KClass<T>): Spec? {
        if (!SpecAnnotations.hasRobolectricTest(clazz)) return null
        val spec = SpecAnnotations.configuration(clazz.java)
        MultiSdkErrors.checkClassLevelSingleSdk(clazz.java, spec.config)
        val runner = SharedRunnerCache.get(spec)
        val bootstrapped = runner.sdkEnvironment.bootstrappedClass<Spec>(clazz.java)
        return bootstrapped.getDeclaredConstructor().newInstance()
    }

    override suspend fun intercept(spec: Spec, execute: suspend (Spec) -> Unit) {
        if (!SpecAnnotations.hasRobolectricTest(spec::class)) {
            execute(spec)
            return
        }
        val runner = SharedRunnerCache.get(SpecAnnotations.configuration(spec::class.java))
        val previous = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = runner.sdkEnvironment.robolectricClassLoader
        runner.containedBefore()
        try {
            execute(spec)
        } finally {
            try {
                runner.containedAfter()
            } finally {
                Thread.currentThread().contextClassLoader = previous
            }
        }
    }
}
