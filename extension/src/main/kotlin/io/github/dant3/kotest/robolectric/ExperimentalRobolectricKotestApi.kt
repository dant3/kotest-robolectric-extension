package io.github.dant3.kotest.robolectric

/**
 * Marks declarations that are experimental in kotest-robolectric-extension.
 *
 * Experimental APIs may change incompatibly between minor releases or be
 * removed entirely. Code using them must explicitly opt in via:
 *
 * ```
 * @OptIn(ExperimentalRobolectricKotestApi::class)
 * ```
 *
 * or via the corresponding compiler flag.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is experimental and may change incompatibly. " +
        "Opt in with @OptIn(ExperimentalRobolectricKotestApi::class).",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
public annotation class ExperimentalRobolectricKotestApi
