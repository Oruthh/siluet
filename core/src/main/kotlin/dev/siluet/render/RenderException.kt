package dev.siluet.render

import dev.siluet.SourceLocation

open class RenderException(
    msg: String,
    val sourceLocation: SourceLocation
) : RuntimeException("$msg at $sourceLocation") {
}