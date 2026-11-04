package dev.siluet.exception

import dev.siluet.SourceLocation

class LexerException(
    msg: String,
    val sourceLocation: SourceLocation,
): RuntimeException("Parse error: $msg at $sourceLocation")