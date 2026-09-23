# Changelog

## 1.1.0

### Upgrading from 1.0.0

- **Dependency coordinates changed.** The coordinates documented for 1.0.0 never resolved on JitPack. Use:
  ```kotlin
  testImplementation("com.github.dant3:kotest-robolectric-extension:1.1.0")
  ```
  `com.github.dant3.kotest-robolectric-extension:extension:<version>` does not exist ([#1](https://github.com/dant3/kotest-robolectric-extension/issues/1)).
- **`@Config` from superclasses is now merged, not replaced.** In 1.0.0 a spec's own `@Config` hid its superclass's `@Config` completely. Now the superclass's `@Config` is applied first and the subclass's `@Config` is laid over it, the same way Robolectric does it under JUnit. If a base spec sets, say, `qualifiers` and a subclass sets only `sdk`, the subclass now gets both. The single-SDK check for class-level `@Config` runs on the merged result.

### Fixed

- `@RobolectricTest` and `@Config` on a superclass (e.g. an abstract base spec) now apply to concrete subclasses. Previously the subclass silently ran outside the Robolectric sandbox and failed with `No instrumentation registered!` ([#2](https://github.com/dant3/kotest-robolectric-extension/issues/2)).
- `withSdks` works on Kotest 6.2. It failed with `NoSuchMethodError: DslDrivenSpec.rootTests()` because Kotest 6.2 renamed that method ([#3](https://github.com/dant3/kotest-robolectric-extension/issues/3)).
- Specs using `isolationMode = IsolationMode.InstancePerRoot` or `InstancePerTest` no longer crash with `NullPointerException: roboMethod.testLifecycle is null` ([#4](https://github.com/dant3/kotest-robolectric-extension/issues/4)).

### Added

- Tests can be isolated from each other with Kotest's isolation modes. With `InstancePerRoot` every root test gets a fresh Application, and with `InstancePerTest` every test does. See [State between tests](README.md#state-between-tests).
- Supported Kotest versions: 6.1.x and 6.2.x. The artifact is compiled against Kotest 6.1 and tested against 6.1.9 and 6.2.5.

### Pitfalls

- **Tests in a spec are not isolated by default.** With the default `SingleInstance` mode all tests of a spec share one Application: SharedPreferences, databases and files written by one test are visible to the next. Robolectric under JUnit gives every test method a fresh Application, so specs migrated from JUnit can silently lose isolation.
- **Static state survives every isolation mode.** Isolation modes recreate the Robolectric test environment (the Application and what hangs off it). They do not recreate the sandbox classloader, so singletons stay alive between tests: Koin's `GlobalContext`, `WorkManager` initialized via `WorkManagerTestInitHelper`, databases held by such singletons, `Dispatchers.setMain`, your own `object`s. Reset them yourself in `beforeTest` / `afterTest`, as shown in the README.
- **`by inject()` keeps the instance from the first Koin graph.** If you restart Koin between tests, the lazy delegate on the spec still returns the instance created by the previous graph, so state leaks between tests of the same spec instance. Use `get()` on every access instead.
- **`beforeTest` / `afterTest` also fire for containers.** `withData` leaves report themselves as containers, so filtering on `TestType.Test` skips them. Keep per-test setup idempotent (e.g. `stopKoin()` before `startKoin {}`).
- **With an isolation mode, `afterSpec` sees a fresh Application**, not the one the tests used.
- **`InstancePerTest` is the most expensive mode.** Kotest re-creates the spec and re-runs the enclosing container bodies for every test, and each run sets up a new Application.

### Not included

- **No explicit reset call.** There is no API to recreate the Application in the middle of a spec. Use an isolation mode, or clear the specific state (preferences, databases, files) yourself.
- **No `@RobolectricTest` lifecycle option.** Per-test isolation is configured with Kotest's `isolationMode`, not with a flag on the annotation.
- **Kotest 7 is not supported.** The extension relies on `DslDrivenSpec` and `add(RootTest)`, which Kotest 6.2 deprecates for removal in 7.0.
- **Not tested:** `isolationMode` set project-wide through `AbstractProjectConfig` (it uses the same per-instance mechanism, so it is expected to work), and running tests of Robolectric specs concurrently.

## 1.0.0

Initial release.

- `@RobolectricTest` runs a Kotest spec inside a Robolectric sandbox.
- Class-level Robolectric `@Config`.
- Experimental multi-SDK tests with the `withSdks` DSL.
