package dev.siluet.exception

/**
 * Base class of exceptions thrown by Siluet when a page cannot be rendered.
 *
 * Catch it to handle any rendering failure, or catch a subclass to learn where the error is:
 * - [SiluetViewException]: an error at a position in an HTML view (layout, section or component),
 *   with the exact [SiluetViewException.sourceLocation],
 * - [SiluetBlueprintException]: an invalid JSON blueprint (template or section group),
 *   with the [SiluetBlueprintException.sourceName] of the file,
 * - [SiluetAssetNotFoundException]: an asset requested by the application does not exist.
 *
 * No partial output is returned when a render fails.
 *
 * @param message Diagnostic message explaining the failure.
 * @param cause The underlying exception, if any.
 */
open class SiluetException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)