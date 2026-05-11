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
 * **Experimental:** the shape of this DSL may change. See [ExperimentalRobolectricKotestApi].
 *
 * Example:
 * ```
 * withSdks("Build.VERSION reports", 23, 28, 30) { sdk ->
 *     Build.VERSION.SDK_INT shouldBe sdk
 * }
 * ```
 */
@ExperimentalRobolectricKotestApi
public fun DslDrivenSpec.withSdks(name: String, vararg sdks: Int, test: suspend TestScope.(sdk: Int) -> Unit) {
    withSdks(sdks = sdks, nameFn = { "$name [SDK $it]" }, test = test)
}

/**
 * Same as [withSdks] but with a custom naming function — useful when the per-SDK
 * test name does not fit the simple `prefix [SDK N]` form.
 *
 * **Experimental:** the shape of this DSL may change. See [ExperimentalRobolectricKotestApi].
 *
 * Example: `withSdks(23, 28, nameFn = { "level $it support" }) { sdk -> ... }`
 */
@ExperimentalRobolectricKotestApi
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

/**
 * Lifecycle contract for [withSdks]'s setup/teardown overload:
 *
 *  - If [setup] throws, [teardown] is NOT run (try-with-resources semantics: there is
 *    nothing to clean up that was never set up).
 *  - If [body] throws, [teardown] runs regardless, and the body's exception propagates.
 *  - If both [body] and [teardown] throw, the body exception propagates with the
 *    teardown failure attached as a suppressed exception.
 *  - If [body] succeeds and [teardown] throws, the teardown failure propagates.
 */
@Suppress("ThrowingExceptionFromFinally") // try-with-resources-like contract documented above
internal suspend fun runSetupTeardown(
    setup: suspend () -> Unit,
    teardown: suspend () -> Unit,
    body: suspend () -> Unit,
) {
    setup()
    var bodyError: Throwable? = null
    try {
        body()
    } catch (t: Throwable) {
        bodyError = t
        throw t
    } finally {
        try {
            teardown()
        } catch (teardownError: Throwable) {
            if (bodyError != null) {
                bodyError.addSuppressed(teardownError)
            } else {
                throw teardownError
            }
        }
    }
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

/**
 * Same as [withSdks] but runs [setup] before and [teardown] after each per-SDK invocation,
 * **inside** the Robolectric sandbox lifecycle (after `containedBefore` and before
 * `containedAfter`). Use this to bridge per-SDK lifecycle for DI frameworks or other
 * global state that needs to be initialized fresh under each sandbox.
 *
 * The [setup] and [teardown] lambdas are captured during the recursive per-SDK bootstrap
 * of the spec, so their class references resolve through the target SDK's sandbox
 * classloader. This is what makes Koin / Hilt / similar frameworks "see" the correct
 * SDK state.
 *
 * **Experimental:** the shape of this DSL may change. See [ExperimentalRobolectricKotestApi].
 *
 * Example (Koin):
 * ```
 * withSdks(
 *     "service is available on each SDK", 23, 28, 30,
 *     setup = { startKoin { modules(myModule) } },
 *     teardown = { stopKoin() },
 * ) { sdk ->
 *     val service by inject<Service>()
 *     service.doStuff()
 * }
 * ```
 */
@ExperimentalRobolectricKotestApi
public fun DslDrivenSpec.withSdks(
    name: String,
    vararg sdks: Int,
    setup: suspend () -> Unit,
    teardown: suspend () -> Unit = {},
    test: suspend TestScope.(sdk: Int) -> Unit,
) {
    withSdks(
        sdks = sdks,
        nameFn = { "$name [SDK $it]" },
        setup = setup,
        teardown = teardown,
        test = test,
    )
}

/**
 * Same as the `name`-prefixed setup-aware [withSdks] but with a custom naming function.
 *
 * **Experimental:** the shape of this DSL may change. See [ExperimentalRobolectricKotestApi].
 */
@ExperimentalRobolectricKotestApi
public fun DslDrivenSpec.withSdks(
    vararg sdks: Int,
    nameFn: (Int) -> String,
    setup: suspend () -> Unit,
    teardown: suspend () -> Unit = {},
    test: suspend TestScope.(sdk: Int) -> Unit,
) {
    require(sdks.isNotEmpty()) { "withSdks requires at least one SDK" }
    val active = SdkBootstrapContext.currentSdk
    if (active != null) {
        if (active in sdks) registerSingleSdkTestWithSetup(active, nameFn(active), setup, teardown, test)
        return
    }
    val classConfig = this::class.java.getAnnotation(Config::class.java)
    sdks.forEach { sdk -> bootstrapAndAttachSdkTestWithSetup(sdk, classConfig, nameFn) }
}

@OptIn(KotestInternal::class)
private fun DslDrivenSpec.registerSingleSdkTestWithSetup(
    sdk: Int,
    name: String,
    setup: suspend () -> Unit,
    teardown: suspend () -> Unit,
    test: suspend TestScope.(sdk: Int) -> Unit,
) {
    val body: suspend TestScope.() -> Unit = {
        runSetupTeardown(setup, teardown) { test(sdk) }
    }
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
            test = body,
            type = TestType.Test,
            source = SourceRef.ClassSource(this::class.java.name),
            xmethod = TestXMethod.NONE,
            config = null,
            factoryId = null,
        ),
    )
}

@OptIn(KotestInternal::class)
private fun DslDrivenSpec.bootstrapAndAttachSdkTestWithSetup(
    sdk: Int,
    classConfig: Config?,
    nameFn: (Int) -> String,
) {
    val runner = SharedRunnerCache.get(classConfig, sdk)
    val perSdkSpec = SdkBootstrapContext.withSdk(sdk) {
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
