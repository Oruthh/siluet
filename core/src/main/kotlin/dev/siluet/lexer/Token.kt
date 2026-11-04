package dev.siluet.lexer

import dev.siluet.SourceLocation

internal data class Token(
    val type: TokenType,
    val value: String,
    val sourceLocation: SourceLocation,
)
