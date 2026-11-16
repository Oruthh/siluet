package dev.siluet.exception.internal

import dev.siluet.SourceLocation

internal class LexerException(
    msg: String,
    val sourceLocation: SourceLocation,
): RuntimeException("Parse error: $msg")