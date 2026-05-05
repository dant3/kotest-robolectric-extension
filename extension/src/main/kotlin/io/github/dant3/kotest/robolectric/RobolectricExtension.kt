package io.github.dant3.kotest.robolectric

import io.github.dant3.kotest.robolectric.internal.ContainedRobolectricRunner
import io.github.dant3.kotest.robolectric.internal.ContainedRunnerCache
import io.github.dant3.kotest.robolectric.internal.SdkResolver
import io.kotest.common.KotestInternal
import io.kotest.core.extensions.ConstructorExtension
import io.kotest.core.extensions.SpecExtension
import io.kotest.core.spec.DslDrivenSpec
import io.kotest.core.spec.RootTest
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestScope
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

@OptIn(KotestInternal::class)
public class RobolectricExtension :
    ConstructorExtension,
    SpecExtension {

    override fun <T : Spec> instantiate(clazz: KClass<T>): Spec? {
        if (!clazz.hasRobolectricTest()) return null
        val config = clazz.java.getAnnotation(Config::class.java)
        val sdks = SdkResolver.resolveSdks(clazz.java)

        if (sdks.size <= 1) {
            val runner = sharedCache.get(config)
            val bootstrapped = runner.sdkEnvironment.bootstrappedClass<Spec>(clazz.java)
            return bootstrapped.getDeclaredConstructor().newInstance()
        }

        return instantiateMultiSdk(clazz, config, sdks)
    }

    override suspend fun intercept(spec: Spec, execute: suspend (Spec) -> Unit) {
        if (!spec::class.hasRobolectricTest()) {
            execute(spec)
            return
        }
        val sdks = SdkResolver.resolveSdks(spec::class.java)
        if (sdks.size > 1) {
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

    private fun <T : Spec> instantiateMultiSdk(clazz: KClass<T>, config: Config?, sdks: List<Int>): Spec {
        val perSdkInstances: List<Pair<Int, Spec>> = sdks.map { apiLevel ->
            val runner = sharedCache.get(config, apiLevel)
            val bootstrapped = runner.sdkEnvironment.bootstrappedClass<Spec>(clazz.java)
            apiLevel to bootstrapped.getDeclaredConstructor().newInstance()
        }
        val primary = perSdkInstances.first().second
        require(primary is DslDrivenSpec) {
            "Multi-SDK currently supports only DslDrivenSpec subclasses, got ${primary::class.qualifiedName}"
        }
        val combined = perSdkInstances.flatMap { (apiLevel, instance) ->
            val runner = sharedCache.get(config, apiLevel)
            val instanceRoots = (instance as DslDrivenSpec).rootTests()
            instanceRoots.map { root -> wrapForSdk(root, apiLevel, runner) }
        }
        replaceRootTests(primary, combined)
        return primary
    }

    private fun wrapForSdk(root: RootTest, apiLevel: Int, runner: ContainedRobolectricRunner): RootTest {
        val originalTest = root.test
        val wrapped: suspend TestScope.() -> Unit = {
            val previous = Thread.currentThread().contextClassLoader
            Thread.currentThread().contextClassLoader = runner.sdkEnvironment.robolectricClassLoader
            runner.containedBefore()
            try {
                originalTest(this)
            } finally {
                try {
                    runner.containedAfter()
                } finally {
                    Thread.currentThread().contextClassLoader = previous
                }
            }
        }
        return root.copy(
            name = root.name.copy(name = "${root.name.name} [SDK $apiLevel]"),
            test = wrapped,
        )
    }

    private fun replaceRootTests(spec: DslDrivenSpec, tests: List<RootTest>) {
        val field = DslDrivenSpec::class.java.getDeclaredField(ROOT_TESTS_FIELD)
        field.isAccessible = true
        field.set(spec, tests)
    }

    private fun KClass<*>.hasRobolectricTest(): Boolean =
        annotations.any { it.annotationClass.qualifiedName == ROBOLECTRIC_TEST_FQN }

    private companion object {
        val ROBOLECTRIC_TEST_FQN: String = RobolectricTest::class.qualifiedName!!
        const val ROOT_TESTS_FIELD = "rootTests"
        val sharedCache = ContainedRunnerCache()
    }
}
