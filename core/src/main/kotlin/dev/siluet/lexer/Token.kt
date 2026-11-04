package dev.siluet.lexer

import dev.siluet.SourceLocation

data class Token(
    val type: TokenType,
    val value: String,
    val sourceLocation: SourceLocation,
)
