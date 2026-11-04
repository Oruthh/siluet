package dev.siluet.exception

import dev.siluet.SourceLocation

internal class ParseException(
    msg: String,
    val sourceLocation: SourceLocation,
) : RuntimeException("Parse error: $msg at $sourceLocation")