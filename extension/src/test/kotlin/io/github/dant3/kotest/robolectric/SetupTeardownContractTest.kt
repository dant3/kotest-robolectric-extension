package io.github.dant3.kotest.robolectric

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private class SetupFailure(message: String) : RuntimeException(message)
private class TeardownFailure(message: String) : RuntimeException(message)
private class BodyFailure(message: String) : RuntimeException(message)

class SetupTeardownContractTest :
    StringSpec({
        "happy path: setup -> body -> teardown, in order" {
            val log = mutableListOf<String>()
            runSetupTeardown(
                setup = { log.add("setup") },
                teardown = { log.add("teardown") },
                body = { log.add("body") },
            )
            log shouldContainExactly listOf("setup", "body", "teardown")
        }

        "body throws, teardown succeeds: body's exception propagates, teardown ran" {
            val log = mutableListOf<String>()
            val error = shouldThrow<BodyFailure> {
                runSetupTeardown(
                    setup = { log.add("setup") },
                    teardown = { log.add("teardown") },
                    body = {
                        log.add("body")
                        throw BodyFailure("body broke")
                    },
                )
            }
            error.message shouldBe "body broke"
            error.suppressed.size shouldBe 0
            log shouldContainExactly listOf("setup", "body", "teardown")
        }

        "body throws and teardown throws: body propagates, teardown attached as suppressed" {
            val log = mutableListOf<String>()
            val error = shouldThrow<BodyFailure> {
                runSetupTeardown(
                    setup = { log.add("setup") },
                    teardown = {
                        log.add("teardown")
                        throw TeardownFailure("teardown broke")
                    },
                    body = {
                        log.add("body")
                        throw BodyFailure("body broke")
                    },
                )
            }
            error.message shouldBe "body broke"
            error.suppressed.size shouldBe 1
            error.suppressed[0].shouldBeInstanceOf<TeardownFailure>()
            error.suppressed[0].message shouldContain "teardown broke"
            log shouldContainExactly listOf("setup", "body", "teardown")
        }

        "body succeeds and teardown throws: teardown's exception propagates" {
            val log = mutableListOf<String>()
            val error = shouldThrow<TeardownFailure> {
                runSetupTeardown(
                    setup = { log.add("setup") },
                    teardown = {
                        log.add("teardown")
                        throw TeardownFailure("teardown broke")
                    },
                    body = { log.add("body") },
                )
            }
            error.message shouldBe "teardown broke"
            log shouldContainExactly listOf("setup", "body", "teardown")
        }

        "setup throws: body and teardown are NOT invoked" {
            val log = mutableListOf<String>()
            val error = shouldThrow<SetupFailure> {
                runSetupTeardown(
                    setup = {
                        log.add("setup")
                        throw SetupFailure("setup broke")
                    },
                    teardown = { log.add("teardown") },
                    body = { log.add("body") },
                )
            }
            error.message shouldBe "setup broke"
            log shouldContainExactly listOf("setup")
        }
    })
