package dev.siluet.exception

import dev.siluet.SourceLocation

class SiluetException(
    message: String,
    val sourceLocation: SourceLocation,
) : RuntimeException(message) {
}