package io.github.dant3.kotest.robolectric

import io.github.dant3.kotest.robolectric.internal.ContainedRobolectricRunner
import io.github.dant3.kotest.robolectric.internal.SdkBootstrapContext
import io.github.dant3.kotest.robolectric.internal.SharedRunnerCache
import io.kotest.common.KotestInternal
import io.kotest.core.names.TestName
import io.kotest.core.source.SourceRef
import io.kotest.core.spec.DslDrivenSpec
import io.kotest.core.spec.RootTest
import io.kotest.core.spec.style.TestXMethod
import io.kotest.core.test.TestScope
import io.kotest.core.test.TestType
import org.robolectric.annotation.Config

/**
 * Register one root test per SDK level. Each test runs inside its own Robolectric
 * sandbox, so SDK-dependent code (Build.VERSION.SDK_INT, version-specific shadows)
 * reflects the actual sandbox state. Test names are formed as `"$name [SDK $sdk]"`.
 *
 * Example:
 * ```
 * withSdks("Build.VERSION reports", 23, 28, 30) { sdk ->
 *     Build.VERSION.SDK_INT shouldBe sdk
 * }
 * ```
 */
public fun DslDrivenSpec.withSdks(name: String, vararg sdks: Int, test: suspend TestScope.(sdk: Int) -> Unit) {
    withSdks(sdks = sdks, nameFn = { "$name [SDK $it]" }, test = test)
}

/**
 * Same as [withSdks] but with a custom naming function — useful when the per-SDK
 * test name does not fit the simple `prefix [SDK N]` form.
 *
 * Example: `withSdks(23, 28, nameFn = { "level $it support" }) { sdk -> ... }`
 */
public fun DslDrivenSpec.withSdks(vararg sdks: Int, nameFn: (Int) -> String, test: suspend TestScope.(sdk: Int) -> Unit) {
    require(sdks.isNotEmpty()) { "withSdks requires at least one SDK" }
    val active = SdkBootstrapContext.currentSdk
    if (active != null) {
        if (active in sdks) registerSingleSdkTest(active, nameFn(active), test)
        return
    }
    val classConfig = this::class.java.getAnnotation(Config::class.java)
    sdks.forEach { sdk -> bootstrapAndAttachSdkTest(sdk, classConfig, nameFn) }
}

@OptIn(KotestInternal::class)
private fun DslDrivenSpec.registerSingleSdkTest(sdk: Int, name: String, test: suspend TestScope.(sdk: Int) -> Unit) {
    add(
        RootTest(
            name = TestName(
                name = name,
                focus = false,
                bang = false,
                prefix = null,
                suffix = null,
                defaultAffixes = false,
            ),
            test = { test(sdk) },
            type = TestType.Test,
            source = SourceRef.ClassSource(this::class.java.name),
            xmethod = TestXMethod.NONE,
            config = null,
            factoryId = null,
        ),
    )
}

@OptIn(KotestInternal::class)
private fun DslDrivenSpec.bootstrapAndAttachSdkTest(sdk: Int, classConfig: Config?, nameFn: (Int) -> String) {
    val runner = SharedRunnerCache.get(classConfig, sdk)
    val perSdkSpec = SdkBootstrapContext.withSdk(sdk) {
        // Spec class is re-bootstrapped under this SDK's sandbox so the lambda captured
        // inside the spec body resolves Android references through the sandbox classloader
        // (Build.VERSION.SDK_INT, shadows, etc.).
        val originalClass = Class.forName(
            this::class.java.name,
            false,
            SharedRunnerCache::class.java.classLoader,
        )
        val bootstrapped = runner.sdkEnvironment.bootstrappedClass<DslDrivenSpec>(originalClass)
        bootstrapped.getDeclaredConstructor().newInstance()
    }
    val expectedName = nameFn(sdk)
    val perSdkRoot = perSdkSpec.rootTests().lastOrNull { it.name.name == expectedName }
        ?: error(
            "Internal error: per-SDK bootstrap for SDK=$sdk produced no rootTest named " +
                "'$expectedName'. This is a bug in kotest-robolectric-extension.",
        )
    add(perSdkRoot.copy(test = wrapWithSandbox(perSdkRoot.test, runner)))
}

private fun wrapWithSandbox(
    inner: suspend TestScope.() -> Unit,
    runner: ContainedRobolectricRunner,
): suspend TestScope.() -> Unit = {
    val previous = Thread.currentThread().contextClassLoader
    Thread.currentThread().contextClassLoader = runner.sdkEnvironment.robolectricClassLoader
    runner.containedBefore()
    try {
        inner()
    } finally {
        try {
            runner.containedAfter()
        } finally {
            Thread.currentThread().contextClassLoader = previous
        }
    }
}
