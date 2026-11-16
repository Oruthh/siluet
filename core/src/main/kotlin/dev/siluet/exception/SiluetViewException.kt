package dev.siluet.exception

import dev.siluet.SourceLocation

class SiluetViewException(
    message: String,
    val sourceLocation: SourceLocation,
    cause: Throwable? = null
): SiluetException("$message at $sourceLocation", cause)