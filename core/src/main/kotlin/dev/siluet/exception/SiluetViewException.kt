package dev.siluet.exception

import dev.siluet.SourceLocation

/**
 * Thrown when rendering fails at a known position in an HTML view (layout, section or component).
 *
 * Covers syntax errors, failing filters and expressions, missing component or section views,
 * unknown section groups in `@sections("...")`, and exceeded loop or render depth limits.
 *
 * The message ends with the location, e.g.
 * `Cannot parse '{}' as a valid date/time at /theme/layouts/main.html:8:19`.
 *
 * @param message Diagnostic message explaining the failure, without the location.
 * @property sourceLocation Source name, line and column of the error. The source name is the
 *   [dev.siluet.loader.SiluetAsset.sourceName] reported by the asset loader.
 * @param cause The underlying exception, if any.
 */
class SiluetViewException(
    message: String,
    val sourceLocation: SourceLocation,
    cause: Throwable? = null
): SiluetException("$message at $sourceLocation", cause)
