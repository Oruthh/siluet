package dev.siluet.exception

class SiluetBlueprintException(message: String, val sourceName: String, cause: Throwable? = null)
    : SiluetException("$message in $sourceName", cause)