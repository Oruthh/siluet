package dev.siluet.exception

import dev.siluet.SourceLocation

internal open class RenderException(
    msg: String,
    val sourceLocation: SourceLocation
) : RuntimeException("$msg at $sourceLocation") {
}