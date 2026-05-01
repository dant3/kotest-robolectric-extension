package io.github.dant3.kotest.robolectric

import io.github.dant3.kotest.robolectric.internal.ContainedRobolectricRunner
import io.kotest.core.extensions.ConstructorExtension
import io.kotest.core.extensions.TestCaseExtension
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import kotlin.reflect.KClass

public class RobolectricExtension :
    ConstructorExtension,
    TestCaseExtension {

    private val runner: ContainedRobolectricRunner by lazy { ContainedRobolectricRunner() }

    override fun <T : Spec> instantiate(clazz: KClass<T>): Spec? {
        if (!clazz.hasRobolectricTest()) return null
        val bootstrapped = runner.sdkEnvironment.bootstrappedClass<Spec>(clazz.java)
        return bootstrapped.getDeclaredConstructor().newInstance()
    }

    override suspend fun intercept(testCase: TestCase, execute: suspend (TestCase) -> TestResult): TestResult {
        if (!testCase.spec::class.hasRobolectricTest()) {
            return execute(testCase)
        }
        val previous = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = runner.sdkEnvironment.robolectricClassLoader
        runner.containedBefore()
        try {
            return execute(testCase)
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
    }
}
