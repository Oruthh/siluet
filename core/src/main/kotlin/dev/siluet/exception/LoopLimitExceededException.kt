package dev.siluet.exception

import dev.siluet.SourceLocation

internal class LoopLimitExceededException(
    val limit: Int,
    sourceLocation: SourceLocation,
): RenderException("Loop iteration limit of $limit exceeded.", sourceLocation)