package io.github.dant3.kotest.robolectric.e2e

import io.github.dant3.kotest.robolectric.RobolectricTest
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get

class InMemoryRepository {
    val items = mutableListOf<String>()
}

@RobolectricTest
class KoinPerTestTest :
    FunSpec(), KoinTest {
    // Not `by inject()`: the lazy delegate would keep the instance from the first graph.
    private val repository: InMemoryRepository get() = get()

    init {
        isolationMode = IsolationMode.InstancePerRoot

        // beforeTest also fires for containers, so the start must be idempotent.
        beforeTest {
            stopKoin()
            startKoin { modules(module { single { InMemoryRepository() } }) }
        }
        afterTest { stopKoin() }

        test("root test gets a fresh graph") {
            repository.items.shouldBeEmpty()
            repository.items += "root"
        }

        context("container") {
            test("leaf 1 gets a fresh graph") {
                repository.items.shouldBeEmpty()
                repository.items += "leaf 1"
            }

            test("leaf 2 gets a fresh graph") {
                repository.items.shouldBeEmpty()
                repository.items += "leaf 2"
            }
        }
    }
}
