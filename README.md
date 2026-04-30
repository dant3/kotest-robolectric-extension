# kotest-robolectric-extension

Расширение для [Kotest](https://kotest.io/), которое запускает JVM-тесты внутри песочницы [Robolectric](http://robolectric.org/), чтобы тестовый код мог обращаться к классам Android-фреймворка (`android.os.Build`, `android.content.Context` и т.д.) без эмулятора и физического устройства.

## Референсные проекты

- [kotest/kotest-extensions-robolectric](https://github.com/kotest/kotest-extensions-robolectric) — архивированный предшественник для Kotest 5.x
- [apter-tech/junit5-robolectric-extension](https://github.com/apter-tech/junit5-robolectric-extension) — аналогичная задача, но для JUnit 5
- [cusxy/electricspock](https://github.com/cusxy/electricspock) — аналог для Spock

## Модули

- `extension/` — сама библиотека (публикуемая)
- `e2e-test/` — end-to-end тесты, проверяющие работу расширения на реальных Android-типах

## Лицензия

Apache License 2.0 — см. [LICENSE](LICENSE).
