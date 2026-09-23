package io.github.dant3.kotest.robolectric.internal

import io.github.dant3.kotest.robolectric.RobolectricTest
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import org.robolectric.annotation.Config

internal object SpecAnnotations {
    private val ROBOLECTRIC_TEST_FQN: String = RobolectricTest::class.qualifiedName!!

    // Kotlin reflection ignores @Inherited, so supertypes are walked explicitly. Matching by name
    // keeps the check independent of which class loader loaded the spec.
    fun hasRobolectricTest(specClass: KClass<*>): Boolean = (sequenceOf(specClass) + specClass.allSuperclasses)
        .any { klass -> klass.annotations.any { it.annotationClass.qualifiedName == ROBOLECTRIC_TEST_FQN } }

    // Mirrors Robolectric's hierarchical lookup: superclass @Config first, each subclass overlaid on top.
    fun config(specClass: Class<*>): Config? = generateSequence(specClass) { it.superclass }
        .mapNotNull { it.getDeclaredAnnotation(Config::class.java) }
        .toList()
        .reversed()
        .reduceOrNull { base, overlay -> Config.Builder(base).overlay(overlay).build() }
}
