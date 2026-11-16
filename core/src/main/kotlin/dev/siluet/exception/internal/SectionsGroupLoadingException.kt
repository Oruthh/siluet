package dev.siluet.exception.internal

internal class SectionsGroupLoadingException(
    message: String,
    val sourceName: String,
): RuntimeException(message)