package io.github.dant3.kotest.robolectric

import io.kotest.core.extensions.ApplyExtension

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ApplyExtension(RobolectricExtension::class)
public annotation class RobolectricTest
