# kotest-robolectric-extension

A [Kotest](https://kotest.io/) extension that runs JVM tests inside a [Robolectric](http://robolectric.org/) sandbox, so test code can use Android framework classes (`android.os.Build`, `android.content.Context`, etc.) without an emulator or a physical device.

## Usage

### Basic example — default SDK

Annotate the spec class with `@RobolectricTest`. No additional setup is needed: the extension wires up a Robolectric sandbox per spec, initializes it before the spec runs and tears it down afterwards.

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

All tests in the spec share the same sandbox (per-spec lifecycle), so state initialized in `containedBefore` (Application, Looper, resources) is visible across tests in the same spec.

### Customizing the sandbox with `@Config`

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

### Running across multiple SDKs

To run the same test body across several SDK levels, declare each multi-SDK group explicitly via `withSdks` inside the spec body. Each group becomes N independent test cases — one per SDK — each running inside its own Robolectric sandbox:

```kotlin
@RobolectricTest
class BuildAcrossSdksTest : StringSpec({
    // Simple form: test name = "Build.VERSION reports [SDK 23]", "[SDK 28]", "[SDK 30]".
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

- Tests inside `withSdks` use a per-test sandbox lifecycle (each iteration gets its own `containedBefore`/`containedAfter`). Tests outside `withSdks` keep the per-spec lifecycle.
- The extension transparently re-bootstraps the spec class once per requested SDK so SDK-dependent code (`Build.VERSION.SDK_INT`, version-specific shadows, etc.) reflects the actual sandbox state.
- Sandboxes are LRU-cached across specs in the JVM; reusing the same SDK across specs is cheap.

## Reference projects

- [kotest/kotest-extensions-robolectric](https://github.com/kotest/kotest-extensions-robolectric) — archived predecessor for Kotest 5.x
- [apter-tech/junit5-robolectric-extension](https://github.com/apter-tech/junit5-robolectric-extension) — same idea, but for JUnit 5
- [cusxy/electricspock](https://github.com/cusxy/electricspock) — equivalent for Spock

## Modules

- `extension/` — the library itself (published)
- `e2e-test/` — end-to-end tests that exercise the extension against real Android types

## License

Apache License 2.0 — see [LICENSE](LICENSE).
