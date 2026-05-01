# kotest-robolectric-extension

A [Kotest](https://kotest.io/) extension that runs JVM tests inside a [Robolectric](http://robolectric.org/) sandbox, so test code can use Android framework classes (`android.os.Build`, `android.content.Context`, etc.) without an emulator or a physical device.

## Reference projects

- [kotest/kotest-extensions-robolectric](https://github.com/kotest/kotest-extensions-robolectric) — archived predecessor for Kotest 5.x
- [apter-tech/junit5-robolectric-extension](https://github.com/apter-tech/junit5-robolectric-extension) — same idea, but for JUnit 5
- [cusxy/electricspock](https://github.com/cusxy/electricspock) — equivalent for Spock

## Modules

- `extension/` — the library itself (published)
- `e2e-test/` — end-to-end tests that exercise the extension against real Android types

## License

Apache License 2.0 — see [LICENSE](LICENSE).
