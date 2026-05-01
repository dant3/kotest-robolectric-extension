package io.github.dant3.kotest.robolectric

import io.github.dant3.kotest.robolectric.internal.ContainedRunnerCache
import io.kotest.core.extensions.ConstructorExtension
import io.kotest.core.extensions.SpecExtension
import io.kotest.core.spec.Spec
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

public class RobolectricExtension :
    ConstructorExtension,
    SpecExtension {

    override fun <T : Spec> instantiate(clazz: KClass<T>): Spec? {
        if (!clazz.hasRobolectricTest()) return null
        val runner = sharedCache.get(clazz.java.getAnnotation(Config::class.java))
        val bootstrapped = runner.sdkEnvironment.bootstrappedClass<Spec>(clazz.java)
        return bootstrapped.getDeclaredConstructor().newInstance()
    }

    override suspend fun intercept(spec: Spec, execute: suspend (Spec) -> Unit) {
        if (!spec::class.hasRobolectricTest()) {
            execute(spec)
            return
        }
        val runner = sharedCache.get(spec::class.java.getAnnotation(Config::class.java))
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

    private fun KClass<*>.hasRobolectricTest(): Boolean =
        annotations.any { it.annotationClass.qualifiedName == ROBOLECTRIC_TEST_FQN }

    private companion object {
        private val ROBOLECTRIC_TEST_FQN: String = RobolectricTest::class.qualifiedName!!
        private val sharedCache = ContainedRunnerCache()
    }
}
