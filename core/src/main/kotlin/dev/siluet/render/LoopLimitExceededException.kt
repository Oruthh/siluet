package dev.siluet.render

import dev.siluet.SourceLocation

class LoopLimitExceededException(
    val limit: Int,
    sourceLocation: SourceLocation,
): RenderException("Loop iteration limit of $limit exceeded.", sourceLocation)