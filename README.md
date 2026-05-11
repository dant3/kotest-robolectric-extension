# kotest-robolectric-extension

A [Kotest](https://kotest.io/) extension that runs JVM tests inside a [Robolectric](http://robolectric.org/) sandbox so test code can use Android framework classes (`android.os.Build`, `android.content.Context`, etc.) without an emulator or a physical device.

**Contents:**

- [Installation](#installation)
- [Quick start](#quick-start)
- [Customizing the sandbox with `@Config`](#customizing-the-sandbox-with-config)
- [Running across multiple SDKs *(experimental)*](#running-across-multiple-sdks-experimental)
- [Per-SDK setup/teardown for DI frameworks *(experimental)*](#per-sdk-setupteardown-for-di-frameworks-experimental)
- [Reference projects](#reference-projects)
- [License](#license)

## Installation

The library is distributed via [JitPack](https://jitpack.io). Add the JitPack repository and the `extension` module as a test dependency.

**`settings.gradle.kts`**

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**`build.gradle.kts`** (of your test module)

```kotlin
dependencies {
    testImplementation("com.github.dant3.kotest-robolectric-extension:extension:<version>")
    testImplementation("io.kotest:kotest-runner-junit5:<kotest-version>")
}
```

Replace `<version>` with one of:

- a release tag (e.g. `0.1.0`) — pinned, reproducible
- a short commit SHA (e.g. `abc1234`) — pinned to a specific commit
- `main-SNAPSHOT` — latest commit on `main` (not recommended for CI)

To find the latest published build see https://jitpack.io/#dant3/kotest-robolectric-extension.

## Quick start

Annotate the spec class with `@RobolectricTest`. No further setup is needed — the extension wires up a Robolectric sandbox per spec, initializes it before the spec runs, and tears it down afterwards.

```kotlin
@RobolectricTest
class BuildVersionTest : StringSpec({
    "android.os.Build is available inside the sandbox" {
        Build.VERSION.SDK_INT shouldBeGreaterThan 0
    }

    "ApplicationProvider returns an Application context" {
        val app = ApplicationProvider.getApplicationContext<Context>()
        app.shouldBeInstanceOf<Application>()
    }
})
```

All tests in the spec share the same sandbox (per-spec lifecycle), so state initialized in `containedBefore` (Application, Looper, resources) stays alive across tests in the same spec.

## Customizing the sandbox with `@Config`

Robolectric's `@Config` annotation is supported at the class level. Pin a specific SDK, override qualifiers, or supply a custom Application:

```kotlin
@RobolectricTest
@Config(sdk = [Build.VERSION_CODES.R])
class PinnedSdkTest : StringSpec({
    "tests run on Android R" {
        Build.VERSION.SDK_INT shouldBe Build.VERSION_CODES.R
    }
})

@RobolectricTest
@Config(qualifiers = "fr")
class FrenchLocaleTest : StringSpec({
    "default locale is French" {
        ApplicationProvider.getApplicationContext<Application>()
            .resources.configuration.locales[0].language shouldBe "fr"
    }
})

@RobolectricTest
@Config(application = TestApplication::class)
class CustomApplicationTest : StringSpec({
    "ApplicationProvider returns the configured TestApplication" {
        ApplicationProvider.getApplicationContext<Application>()
            .shouldBeInstanceOf<TestApplication>()
    }
})
```

Class-level `@Config` must pin a **single** SDK — multi-SDK values (`sdk = [21, 28]`, `sdk = [Config.ALL_SDKS]`, `minSdk`/`maxSdk` ranges) are rejected with `IllegalArgumentException` that points to the `withSdks` DSL described below.

## Running across multiple SDKs *(experimental)*

> The `withSdks` DSL is annotated with `@ExperimentalRobolectricKotestApi` and requires opt-in via `@OptIn(ExperimentalRobolectricKotestApi::class)` on the spec class or the consuming code. Its shape may change between minor releases.

To run the same test body across several SDK levels, declare each multi-SDK group explicitly via `withSdks` inside the spec body. Each group becomes N independent test cases — one per SDK — each running inside its own Robolectric sandbox:

```kotlin
@RobolectricTest
@OptIn(ExperimentalRobolectricKotestApi::class)
class BuildAcrossSdksTest : StringSpec({
    // Simple form: test names become "Build.VERSION reports [SDK 23]", "[SDK 28]", "[SDK 30]".
    withSdks("Build.VERSION reports", 23, 28, 30) { sdk ->
        Build.VERSION.SDK_INT shouldBe sdk
    }

    // Custom naming when the simple "prefix [SDK N]" form does not fit.
    withSdks(23, 28, nameFn = { "level $it support" }) { sdk ->
        Build.VERSION.SDK_INT shouldBe sdk
    }

    // Single-SDK tests outside withSdks keep the per-spec lifecycle.
    "default sandbox is initialized" {
        Build.VERSION.SDK_INT shouldBeGreaterThan 0
    }
})
```

Notes:

- Tests inside `withSdks` use a **per-test** sandbox lifecycle (each iteration gets its own `containedBefore`/`containedAfter`). Tests outside `withSdks` keep the per-spec lifecycle.
- The extension transparently re-bootstraps the spec class once per requested SDK so SDK-dependent code (`Build.VERSION.SDK_INT`, version-specific shadows, etc.) reflects the actual sandbox state.
- Sandboxes are LRU-cached across specs in the JVM, so reusing the same SDK across specs is cheap.

## Per-SDK setup/teardown for DI frameworks *(experimental)*

DI frameworks with global singletons (Koin's `GlobalContext`) or per-instance graphs (Hilt's `HiltTestApplication`) need their initialization to happen **inside** each per-SDK sandbox — otherwise they live in the default sandbox classloader and the multi-SDK tests either fail with "not started" errors or silently observe the wrong SDK state.

`withSdks` accepts optional `setup` / `teardown` lambdas for exactly this scenario. The lambdas are captured during the recursive per-SDK bootstrap of the spec, so their class references resolve through the target SDK's sandbox classloader.

### Koin

```kotlin
@RobolectricTest
@OptIn(ExperimentalRobolectricKotestApi::class)
class KoinAcrossSdksTest : StringSpec({
    withSdks(
        "service is available on each SDK", 23, 28, 30,
        setup = {
            startKoin {
                modules(
                    module { factory { GreetingService("hello") } },
                )
            }
        },
        teardown = { stopKoin() },
    ) { sdk ->
        Build.VERSION.SDK_INT shouldBe sdk
        val service = object : KoinComponent {}.get<GreetingService>()
        service.greet() shouldBe "hello"
    }
})
```

`startKoin` registers Koin in the per-SDK sandbox's `GlobalContext`; `stopKoin` cleans it up after the test. Without `teardown`, the next test under a different SDK would see the previous run's Koin still "started" inside its own sandbox classloader.

### Hilt

```kotlin
@RobolectricTest
@Config(application = HiltTestApplication::class)
@HiltAndroidTest
@OptIn(ExperimentalRobolectricKotestApi::class)
class HiltAcrossSdksTest : StringSpec() {

    @Inject lateinit var service: SomeService
    private val hiltRule = HiltAndroidRule(this)

    init {
        withSdks(
            "service is injected on each SDK", 23, 28, 30,
            setup = { hiltRule.inject() },
            // No teardown needed — Hilt graph lives in HiltTestApplication, which is
            // recreated per sandbox by Robolectric's containedBefore/After.
        ) { sdk ->
            Build.VERSION.SDK_INT shouldBe sdk
            service.doStuff()
        }
    }
}
```

Each per-SDK iteration creates a fresh `HiltAndroidRule` (via the recursive bootstrap re-running the spec body under that SDK's sandbox), and `hiltRule.inject()` populates the `@Inject lateinit var` fields against the sandbox-local `HiltTestApplication`.

### Exception semantics

The setup/teardown contract follows try-with-resources:

- If `setup` throws, `teardown` is **not** run.
- If the test body throws and `teardown` succeeds, the body's exception propagates.
- If both the body and `teardown` throw, the body's exception propagates with the teardown failure attached as a suppressed exception.
- If the body succeeds and `teardown` throws, the teardown's exception propagates.

### Why is this experimental?

`withSdks` is implemented by re-bootstrapping the spec class once per SDK and stitching the resulting root tests together via reflection on `DslDrivenSpec`. Kotest does not yet expose a public extension point for declaring N runtime variants of a single spec class. The user-visible DSL is intended to stay roughly the same once Kotest adds such an API, but breaking internal changes are possible until then.

## Reference projects

- [kotest/kotest-extensions-robolectric](https://github.com/kotest/kotest-extensions-robolectric) — archived predecessor for Kotest 5.x
- [apter-tech/junit5-robolectric-extension](https://github.com/apter-tech/junit5-robolectric-extension) — same idea, but for JUnit 5
- [cusxy/electricspock](https://github.com/cusxy/electricspock) — equivalent for Spock

## License

Apache License 2.0 — see [LICENSE](LICENSE).
