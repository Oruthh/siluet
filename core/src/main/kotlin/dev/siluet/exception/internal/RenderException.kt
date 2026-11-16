package dev.siluet.exception.internal

import dev.siluet.SourceLocation

internal open class RenderException(
    msg: String,
    val sourceLocation: SourceLocation
) : RuntimeException(msg)