package dev.siluet.exception

import dev.siluet.SourceLocation

/**
 * Base runtime exception thrown when an error occurs during template tokenization,
 * parsing, compilation, or execution in Siluet.
 *
 * Retains a [sourceLocation] reference pointing to the exact template file, line,
 * and column where the error originated to facilitate diagnostic reporting and debugging.
 *
 * @param message Diagnostic message explaining the underlying failure.
 * @property sourceLocation The exact coordinates within the source template where the error occurred.
 */
class SiluetException(
    message: String,
    val sourceLocation: SourceLocation,
) : RuntimeException(message) {
}