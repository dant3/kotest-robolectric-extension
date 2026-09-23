# Changelog

## 1.1.0

This release makes the extension usable for larger test suites. You can now choose how isolated tests are from each other, put `@RobolectricTest` / `@Config` on a shared base spec, and run on Kotest 6.2. It also fixes the dependency coordinates, which never resolved in 1.0.0.

### Highlights

- **Choose the test lifecycle.** A fresh Application per spec, per root test or per test, selected with Kotest's standard `isolationMode`. In 1.0.0 only the per-spec lifecycle worked; the other modes crashed.
- **Base specs.** `@RobolectricTest` and `@Config` declared on an abstract base spec now apply to every subclass, and `@Config` is merged across the hierarchy.
- **Kotest 6.2.** `withSdks` works on Kotest 6.2. Kotest 6.1.x and 6.2.x are supported, and CI tests both on every change.

### Behavior changes compared to 1.0.0

Most existing specs behave exactly as before, and the default lifecycle is still one Application per spec. The exceptions are:

1. **Subclasses of a `@RobolectricTest` spec now run inside the sandbox.**
   - *1.0.0:* the annotation counted only on the class that declared it. A subclass without its own `@RobolectricTest` silently ran on the plain JVM, with no Android framework available.
   - *1.1.0:* the annotation is inherited, so such subclasses run in Robolectric.
   - *What to do:* nothing, if the subclasses used Android APIs, because they failed before. Subclasses that passed only because they never touched Android now start a sandbox: they get slower, and code under test that checks for Android at runtime takes its Android path. If that is not wanted, move those specs off the annotated base.
2. **`@Config` on a base class and a subclass is merged instead of replaced.**
   - *1.0.0:* a subclass's own `@Config` hid the base's `@Config` completely, including in `withSdks`.
   - *1.1.0:* the base's `@Config` is applied first and the subclass's is laid over it, as Robolectric does under JUnit:
     - `sdk` / `minSdk` / `maxSdk`, `application`, `manifest`, `fontScale`: the subclass's value wins where it sets one; otherwise the base's is kept;
     - `qualifiers`: the subclass's value replaces the base's, unless it starts with `+`, in which case it is appended (`"+land"`);
     - `shadows`, `instrumentedPackages`: combined from both.

     Example: base `@Config(sdk = [30], shadows = [ShadowFoo::class])` plus subclass `@Config(qualifiers = "fr")` used to run on the default SDK without `ShadowFoo`; now it runs on SDK 30, with `ShadowFoo`, in `fr`.
   - *What to do:* review specs that declare `@Config` at more than one level of their hierarchy. Also note that the rule "a class-level `@Config` selects a single SDK" now applies to the merged result: an abstract base with `sdk = [28, 30]` used to be ignored once the subclass had its own `@Config`; now the subclass is rejected unless it sets its own `sdk` (or `minSdk` / `maxSdk`).
3. **`InstancePerRoot` / `InstancePerTest` specs pass instead of crashing.**
   - *1.0.0:* the test bodies ran, but the spec then failed with `NullPointerException: roboMethod.testLifecycle is null`.
   - *1.1.0:* each spec instance gets its own fresh Application, and `afterSpec` gets a fresh one too, not the one the tests used.

### Upgrading from 1.0.0

1. Switch to the new coordinates. The ones documented for 1.0.0 (`com.github.dant3.kotest-robolectric-extension:extension`) never resolved on JitPack ([#1](https://github.com/dant3/kotest-robolectric-extension/issues/1)):
   ```kotlin
   testImplementation("com.github.dant3:kotest-robolectric-extension:1.1.0")
   ```
2. Go through the [behavior changes](#behavior-changes-compared-to-100) above if you use base specs.
3. Optionally, pick an isolation mode for specs whose tests write to the Application's storage (see the next section).

### New: choose how tests are isolated

Until now every test of a spec shared one Robolectric test environment: one Application, so SharedPreferences, databases and files written by one test were visible to the next. That is still the default. Setting a Kotest isolation mode now gives tests a fresh environment ([#4](https://github.com/dant3/kotest-robolectric-extension/issues/4)):

| `isolationMode` | Fresh Application for | Cost |
|---|---|---|
| `SingleInstance` (default) | each spec | lowest |
| `InstancePerRoot` | each root test; tests nested in it share one | one spec instance per root test |
| `InstancePerTest` | each test, containers included | one spec instance per test; enclosing container bodies run again for every test |

```kotlin
@RobolectricTest
class SettingsRepositoryTest : FunSpec({
    isolationMode = IsolationMode.InstancePerRoot

    test("stores the theme") { /* writes SharedPreferences */ }
    test("starts with defaults") { /* sees empty SharedPreferences */ }
})
```

`InstancePerRoot` is the closest match to Robolectric under JUnit, where every test method gets a fresh Application. Pick it for specs that write to the Application's storage. Keep the default for specs that only read, since it is the fastest.

An isolation mode resets what Robolectric owns: the Application and everything obtained from it. It does **not** reset static state in your code or libraries: Koin's `GlobalContext`, `WorkManager` set up with `WorkManagerTestInitHelper`, databases held by singletons, `Dispatchers.setMain`, your own `object`s. The sandbox classloader is shared, so these outlive a test in every mode, just as they do under JUnit. Reset them in `beforeTest` / `afterTest`, as shown in the next section.

### New: shared base specs

`@RobolectricTest` and `@Config` can live on an abstract base spec. Subclasses no longer need to repeat them ([#2](https://github.com/dant3/kotest-robolectric-extension/issues/2)). Together with an isolation mode, this puts the whole test setup in one place:

```kotlin
@RobolectricTest
@Config(sdk = [Build.VERSION_CODES.R])
abstract class DatabaseSpec(body: DatabaseSpec.() -> Unit) : FunSpec(), KoinTest {
    // Not `by inject()`: the lazy delegate would keep the instance from a previous graph.
    val repository: Repository get() = get()

    init {
        isolationMode = IsolationMode.InstancePerRoot

        // Registered before the subclass body, so the subclass's own beforeTest sees a running graph.
        beforeTest {
            stopKoin() // beforeTest also fires for containers: keep the start idempotent
            startKoin {
                androidContext(ApplicationProvider.getApplicationContext())
                modules(appModule)
            }
        }
        afterTest { stopKoin() }

        body()
    }
}

class RepositoryTest : DatabaseSpec({
    test("starts empty") { repository.all().shouldBeEmpty() }
})
```

`@Config` is merged the way Robolectric merges it under JUnit: the base class's `@Config` first, each subclass's on top of it (details in [behavior changes](#behavior-changes-compared-to-100)). A base that pins `sdk` and a subclass that sets `qualifiers = "fr"` run on that SDK with the French locale.

### Kotest 6.2 support

`withSdks` failed on Kotest 6.2 with `NoSuchMethodError: DslDrivenSpec.rootTests()` ([#3](https://github.com/dant3/kotest-robolectric-extension/issues/3)). The same artifact now works on 6.1.x and 6.2.x: it is compiled against Kotest 6.1, and CI runs the end-to-end suite against 6.1.9 and 6.2.5.

### Fixed

- Subclasses of a spec annotated with `@RobolectricTest` ran outside the sandbox and failed with `No instrumentation registered!` ([#2](https://github.com/dant3/kotest-robolectric-extension/issues/2)).
- `withSdks` threw `NoSuchMethodError` on Kotest 6.2 ([#3](https://github.com/dant3/kotest-robolectric-extension/issues/3)).
- `InstancePerRoot` / `InstancePerTest` specs crashed with `NullPointerException: roboMethod.testLifecycle is null` ([#4](https://github.com/dant3/kotest-robolectric-extension/issues/4)).
- The README's dependency coordinates did not resolve ([#1](https://github.com/dant3/kotest-robolectric-extension/issues/1)).

### Things to watch for

- **Migrating from JUnit silently loses isolation** unless you set an isolation mode. Nothing fails; a later test simply sees an earlier test's data.
- **`by inject()` on a spec keeps the first instance it resolved.** If you restart Koin between tests, access dependencies with `get()` instead.
- **`beforeTest` / `afterTest` fire for containers too**, and `withData` leaves report themselves as containers. Do not filter on `TestType.Test`; make the setup idempotent.
- **With an isolation mode, `afterSpec` gets a fresh Application**, not the one the tests used.

### Not in this release

- **No call to reset the Application in the middle of a spec.** Use an isolation mode, or clear the specific state (preferences, databases, files) yourself.
- **Kotest 7 is not supported.** The extension relies on `DslDrivenSpec` and `add(RootTest)`, which Kotest 6.2 deprecates for removal in 7.0.
- **Not tested yet:** combining `withSdks` with an isolation mode, setting `isolationMode` project-wide in `AbstractProjectConfig`, and running Robolectric specs concurrently.

## 1.0.0

Initial release.

- `@RobolectricTest` runs a Kotest spec inside a Robolectric sandbox.
- Class-level Robolectric `@Config`.
- Experimental multi-SDK tests with the `withSdks` DSL.
