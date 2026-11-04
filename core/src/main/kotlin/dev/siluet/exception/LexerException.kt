package dev.siluet.exception

import dev.siluet.SourceLocation

internal class LexerException(
    msg: String,
    val sourceLocation: SourceLocation,
): RuntimeException("Parse error: $msg at $sourceLocation")